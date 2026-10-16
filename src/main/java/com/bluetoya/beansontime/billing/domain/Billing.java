package com.bluetoya.beansontime.billing.domain;

import com.bluetoya.beansontime.billing.domain.exception.InvalidBillingStateChangeException;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

@Getter
public class Billing {
  private final BillingId id;
  private final CustomerId customerId;
  private final SubscriptionId subscriptionId;
  private final ProductId productId;
  private final Money amount;
  private final LocalDate billingDate;
  private final LocalDateTime createdAt;
  private final LocalDateTime expiresAt;
  private BillingStatus status;

  public Billing(
      CustomerId customerId,
      SubscriptionId subscriptionId,
      ProductId productId,
      Money amount,
      LocalDate billingDate,
      LocalDateTime createdAt) {
    this(
        BillingId.generate(),
        customerId,
        subscriptionId,
        productId,
        amount,
        billingDate,
        createdAt,
        createdAt.plusMinutes(10),
        BillingStatus.PENDING);
  }

  private Billing(
      BillingId id,
      CustomerId customerId,
      SubscriptionId subscriptionId,
      ProductId productId,
      Money amount,
      LocalDate billingDate,
      LocalDateTime createdAt,
      LocalDateTime expiresAt,
      BillingStatus status) {
    this.id = Objects.requireNonNull(id);
    this.customerId = Objects.requireNonNull(customerId, "청구 고객 ID는 필수입니다.");
    this.subscriptionId = Objects.requireNonNull(subscriptionId, "청구 구독 ID는 필수입니다.");
    this.productId = Objects.requireNonNull(productId, "청구 상품 ID는 필수입니다.");
    this.amount = Objects.requireNonNull(amount, "청구 금액은 필수입니다.");
    this.billingDate = Objects.requireNonNull(billingDate, "청구일은 필수입니다.");
    this.createdAt = Objects.requireNonNull(createdAt, "청구 생성 시각은 필수입니다.");
    this.expiresAt = Objects.requireNonNull(expiresAt);
    this.status = Objects.requireNonNull(status);
    if (!expiresAt.equals(createdAt.plusMinutes(10))) {
      throw new IllegalArgumentException("청구 유효기간은 생성 후 10분이어야 합니다.");
    }
  }

  public static Billing restore(
      BillingId id,
      CustomerId customerId,
      SubscriptionId subscriptionId,
      ProductId productId,
      Money amount,
      LocalDate billingDate,
      LocalDateTime createdAt,
      LocalDateTime expiresAt,
      BillingStatus status) {
    return new Billing(
        id,
        customerId,
        subscriptionId,
        productId,
        amount,
        billingDate,
        createdAt,
        expiresAt,
        status);
  }

  public boolean isPaymentWindowClosed(LocalDateTime now) {
    return !Objects.requireNonNull(now, "판단 시각은 필수입니다.").isBefore(expiresAt);
  }

  public void expireIfDue(LocalDateTime now, boolean paymentProcessing) {
    if (status == BillingStatus.PENDING && isPaymentWindowClosed(now) && !paymentProcessing) {
      status = BillingStatus.EXPIRED;
    }
  }

  public void cancelAfterCompensation() {
    if (status == BillingStatus.CANCELLED) return;
    if (status != BillingStatus.PENDING)
      throw new InvalidBillingStateChangeException("보상 취소할 청구 상태가 아닙니다.");
    status = BillingStatus.CANCELLED;
  }

  public void markPaid() {
    if (status == BillingStatus.EXPIRED || status == BillingStatus.CANCELLED) {
      throw new InvalidBillingStateChangeException("만료된 청구는 결제 완료할 수 없습니다.");
    }
    this.status = BillingStatus.PAID;
  }
}
