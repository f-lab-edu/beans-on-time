package com.bluetoya.beansontime.subscription.domain;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPausePeriodException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPeriodStateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionResumeDateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionStateChangeException;
import com.bluetoya.beansontime.subscription.domain.exception.SubscriptionResumeRequiresPaymentException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Subscription {
  private final SubscriptionId id;
  private final CustomerId customerId;
  private final ProductId productId;
  private final DeliveryCycle deliveryCycle;
  private final LocalDate startedDate;
  private BillingAnchorDay billingAnchorDay;

  @Getter(AccessLevel.NONE)
  private final EnumSet<SubscriptionSuspensionReason> suspensionReasons;

  private SubscriptionPeriod currentPeriod;
  private Integer remainingPaidDays;
  private LocalDate nextBillingDate;
  private LocalDateTime pausedAt;
  private LocalDate scheduledResumeDate;
  private SubscriptionStatus lifecycleStatus;

  public Subscription(
      CustomerId customerId,
      ProductId productId,
      DeliveryCycle deliveryCycle,
      LocalDate startedDate) {
    this.id = SubscriptionId.generate();
    this.customerId = Objects.requireNonNull(customerId, "고객 ID는 필수입니다.");
    this.productId = Objects.requireNonNull(productId, "상품 ID는 필수입니다.");
    this.deliveryCycle = Objects.requireNonNull(deliveryCycle, "납품 주기는 필수입니다.");
    this.startedDate = Objects.requireNonNull(startedDate, "구독 시작일은 필수입니다.");
    this.billingAnchorDay = BillingAnchorDay.from(startedDate);
    this.nextBillingDate = billingAnchorDay.nextBillingDateAfter(startedDate);
    this.currentPeriod = new SubscriptionPeriod(startedDate, this.nextBillingDate.minusDays(1));
    this.suspensionReasons = EnumSet.noneOf(SubscriptionSuspensionReason.class);
    this.lifecycleStatus = SubscriptionStatus.ACTIVE;
  }

  public void pause(LocalDate pauseUntilDate, LocalDateTime pausedAt) {
    if (this.lifecycleStatus != SubscriptionStatus.ACTIVE) {
      throw new InvalidSubscriptionStateChangeException("일시정지 불가능한 구독입니다.");
    }

    Objects.requireNonNull(pauseUntilDate, "일시정지 종료일은 필수입니다.");

    if (pauseUntilDate.isAfter(LocalDate.now().plusMonths(3))) {
      throw new InvalidSubscriptionPausePeriodException("일시정지 종료일은 3개월 이내여야 합니다.");
    }

    Objects.requireNonNull(pausedAt, "일시정지 요청 시각은 필수입니다.");

    LocalDate pauseDate = pausedAt.toLocalDate();
    if (pauseUntilDate.isBefore(pauseDate)) {
      throw new InvalidSubscriptionPausePeriodException("일시정지 종료일은 요청일보다 빠를 수 없습니다.");
    }

    SubscriptionPeriod paidPeriod = currentPeriod;
    if (paidPeriod == null
        || pauseDate.isBefore(paidPeriod.startDate())
        || pauseDate.isAfter(paidPeriod.endDate())) {
      throw new InvalidSubscriptionPeriodStateException(
          "ACTIVE 구독의 일시정지 요청일은 현재 이용 구간 안에 있어야 합니다.");
    }

    int remainingDays = Math.toIntExact(ChronoUnit.DAYS.between(pauseDate, paidPeriod.endDate()));
    LocalDate scheduledDate = pauseUntilDate.plusDays(1);

    this.currentPeriod = null;
    this.remainingPaidDays = remainingDays;
    this.pausedAt = pausedAt;
    this.scheduledResumeDate = scheduledDate;
    this.nextBillingDate = scheduledDate.plusDays(remainingDays);
    this.lifecycleStatus = SubscriptionStatus.PAUSED;
  }

  public void resume(LocalDate resumedDate) {
    if (this.lifecycleStatus != SubscriptionStatus.PAUSED) {
      throw new InvalidSubscriptionStateChangeException("구독 재개가 불가능합니다.");
    }

    Objects.requireNonNull(resumedDate, "구독 재개일은 필수입니다.");

    if (currentPeriod != null
        || remainingPaidDays == null
        || pausedAt == null
        || scheduledResumeDate == null
        || nextBillingDate == null) {
      throw new InvalidSubscriptionPeriodStateException("PAUSED 구독의 선결제 이용 기간 문맥이 올바르지 않습니다.");
    }

    LocalDate pauseDate = pausedAt.toLocalDate();
    if (resumedDate.isBefore(pauseDate)) {
      throw new InvalidSubscriptionResumeDateException("구독 재개일은 일시정지 요청일보다 빠를 수 없습니다.");
    }

    int paidDays = remainingPaidDays;
    if (paidDays == 0 && resumedDate.isAfter(pauseDate)) {
      throw new SubscriptionResumeRequiresPaymentException(
          "남아 있는 선결제 이용 기간이 없어 구독 재개 전에 새 결제가 필요합니다.");
    }

    LocalDate baseNextBillingDate = pauseDate.plusDays((long) paidDays + 1);
    long frozenDays = Math.max(0, ChronoUnit.DAYS.between(pauseDate.plusDays(1), resumedDate));
    LocalDate actualNextBillingDate = baseNextBillingDate.plusDays(frozenDays);

    this.currentPeriod = new SubscriptionPeriod(resumedDate, actualNextBillingDate.minusDays(1));
    this.nextBillingDate = actualNextBillingDate;
    if (frozenDays > 0) {
      this.billingAnchorDay = BillingAnchorDay.from(actualNextBillingDate);
    }

    this.remainingPaidDays = null;
    this.pausedAt = null;
    this.scheduledResumeDate = null;
    this.lifecycleStatus = SubscriptionStatus.ACTIVE;
  }

  public boolean isPaidReactivationTarget() {
    return lifecycleStatus == SubscriptionStatus.PAUSED
        && Integer.valueOf(0).equals(remainingPaidDays);
  }

  public void reactivateAfterPayment(LocalDate paymentDate) {
    if (this.lifecycleStatus != SubscriptionStatus.PAUSED || this.remainingPaidDays == null) {
      throw new InvalidSubscriptionStateChangeException("결제로 재활성화할 수 없는 구독입니다.");
    }

    if (this.remainingPaidDays != 0) {
      throw new InvalidSubscriptionStateChangeException("남은 선결제 이용 기간이 있는 구독입니다.");
    }

    Objects.requireNonNull(paymentDate, "결제 성공일은 필수입니다.");

    if (currentPeriod != null
        || pausedAt == null
        || scheduledResumeDate == null
        || nextBillingDate == null) {
      throw new InvalidSubscriptionPeriodStateException("PAUSED 구독의 선결제 이용 기간 문맥이 올바르지 않습니다.");
    }

    if (paymentDate.isBefore(pausedAt.toLocalDate())) {
      throw new InvalidSubscriptionResumeDateException("결제 성공일은 일시정지 요청일보다 빠를 수 없습니다.");
    }

    BillingAnchorDay newBillingAnchorDay = BillingAnchorDay.from(paymentDate);
    LocalDate newNextBillingDate = newBillingAnchorDay.nextBillingDateAfter(paymentDate);

    this.billingAnchorDay = newBillingAnchorDay;
    this.currentPeriod = new SubscriptionPeriod(paymentDate, newNextBillingDate.minusDays(1));
    this.nextBillingDate = newNextBillingDate;
    this.remainingPaidDays = null;
    this.pausedAt = null;
    this.scheduledResumeDate = null;
    this.lifecycleStatus = SubscriptionStatus.ACTIVE;
  }

  public void cancel() {
    this.lifecycleStatus = SubscriptionStatus.CANCELLED;
    this.currentPeriod = null;
    this.remainingPaidDays = null;
    this.nextBillingDate = null;
    this.pausedAt = null;
    this.scheduledResumeDate = null;
  }

  public void addSuspensionReason(SubscriptionSuspensionReason reason) {
    suspensionReasons.add(Objects.requireNonNull(reason, "실행 차단 사유는 필수입니다."));
  }

  public void removeSuspensionReason(SubscriptionSuspensionReason reason) {
    suspensionReasons.remove(Objects.requireNonNull(reason, "실행 차단 사유는 필수입니다."));
  }

  public boolean isExecutionBlocked() {
    return lifecycleStatus != SubscriptionStatus.ACTIVE || !suspensionReasons.isEmpty();
  }

  public Set<SubscriptionSuspensionReason> getSuspensionReasons() {
    return Set.copyOf(suspensionReasons);
  }
}
