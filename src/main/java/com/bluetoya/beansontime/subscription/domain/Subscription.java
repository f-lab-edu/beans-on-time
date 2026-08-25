package com.bluetoya.beansontime.subscription.domain;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPausePeriodException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionResumeDateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionStateChangeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
  private final Cycle cycle;
  private final LocalDate startedDate;
  private final BillingAnchorDay billingAnchorDay;

  @Getter(AccessLevel.NONE)
  private final EnumSet<SubscriptionSuspensionReason> suspensionReasons;

  private SubscriptionPeriod currentPeriod;
  private LocalDate nextBillingDate;
  private LocalDateTime pausedAt;
  private LocalDate resumeDate;
  private SubscriptionStatus lifecycleStatus;

  public Subscription(
      CustomerId customerId, ProductId productId, Cycle cycle, LocalDate startedDate) {
    this.id = SubscriptionId.generate();
    this.customerId = Objects.requireNonNull(customerId, "고객 ID는 필수입니다.");
    this.productId = Objects.requireNonNull(productId, "상품 ID는 필수입니다.");
    this.cycle = Objects.requireNonNull(cycle, "납품 주기는 필수입니다.");
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
    Objects.requireNonNull(pausedAt, "일시정지 요청 시각은 필수입니다.");

    if (pauseUntilDate.isBefore(pausedAt.toLocalDate())) {
      throw new InvalidSubscriptionPausePeriodException("일시정지 종료일은 요청일보다 빠를 수 없습니다.");
    }

    this.pausedAt = pausedAt;
    this.resumeDate = pauseUntilDate.plusDays(1);
    this.lifecycleStatus = SubscriptionStatus.PAUSED;
  }

  public void resume(LocalDate resumedDate) {
    if (this.lifecycleStatus != SubscriptionStatus.PAUSED) {
      throw new InvalidSubscriptionStateChangeException("구독 재개가 불가능합니다.");
    }

    Objects.requireNonNull(resumedDate, "구독 재개일은 필수입니다.");

    if (resumedDate.isBefore(pausedAt.toLocalDate())) {
      throw new InvalidSubscriptionResumeDateException("구독 재개일은 일시정지 요청일보다 빠를 수 없습니다.");
    }

    if (resumedDate.isAfter(currentPeriod.endDate())) {
      this.nextBillingDate = billingAnchorDay.nextBillingDateAfter(resumedDate);
      this.currentPeriod = new SubscriptionPeriod(resumedDate, this.nextBillingDate.minusDays(1));
    }

    this.pausedAt = null;
    this.resumeDate = null;
    this.lifecycleStatus = SubscriptionStatus.ACTIVE;
  }

  public void cancel() {
    this.lifecycleStatus = SubscriptionStatus.CANCELLED;
    this.pausedAt = null;
    this.resumeDate = null;
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
