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
  private final BillingPurpose purpose;
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
        BillingStatus.PENDING,
        BillingPurpose.REACTIVATION);
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
      BillingStatus status,
      BillingPurpose purpose) {
    this.id = Objects.requireNonNull(id);
    this.customerId = Objects.requireNonNull(customerId, "청구 고객 ID는 필수입니다.");
    this.subscriptionId = Objects.requireNonNull(subscriptionId, "청구 구독 ID는 필수입니다.");
    this.productId = Objects.requireNonNull(productId, "청구 상품 ID는 필수입니다.");
    this.amount = Objects.requireNonNull(amount, "청구 금액은 필수입니다.");
    this.billingDate = Objects.requireNonNull(billingDate, "청구일은 필수입니다.");
    this.createdAt = Objects.requireNonNull(createdAt, "청구 생성 시각은 필수입니다.");
    this.purpose = Objects.requireNonNull(purpose);
    this.expiresAt = expiresAt;
    this.status = Objects.requireNonNull(status);
    if (purpose == BillingPurpose.RECURRING
        && (expiresAt != null || status == BillingStatus.EXPIRED)) {
      throw new IllegalArgumentException("정기 청구에는 결제 시작 만료를 적용하지 않습니다.");
    }
    if (purpose == BillingPurpose.REACTIVATION && !createdAt.plusMinutes(10).equals(expiresAt)) {
      throw new IllegalArgumentException("재활성화 청구 유효기간은 생성 후 10분이어야 합니다.");
    }
  }

  public static Billing recurring(
      CustomerId customerId,
      SubscriptionId subscriptionId,
      ProductId productId,
      Money amount,
      LocalDate dueDate,
      LocalDateTime createdAt) {
    return new Billing(
        BillingId.generate(),
        customerId,
        subscriptionId,
        productId,
        amount,
        dueDate,
        createdAt,
        null,
        BillingStatus.PENDING,
        BillingPurpose.RECURRING);
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
      BillingStatus status,
      BillingPurpose purpose) {
    return new Billing(
        id,
        customerId,
        subscriptionId,
        productId,
        amount,
        billingDate,
        createdAt,
        expiresAt,
        status,
        purpose);
  }

  public boolean isPaymentWindowClosed(LocalDateTime now) {
    Objects.requireNonNull(now, "판단 시각은 필수입니다.");
    return expiresAt != null && !now.isBefore(expiresAt);
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

  public void refund() {
    if (status == BillingStatus.REFUNDED) return;
    if (status != BillingStatus.PAID)
      throw new InvalidBillingStateChangeException("완료된 청구만 환불할 수 있습니다.");
    status = BillingStatus.REFUNDED;
  }

  public void markPaid() {
    if (status == BillingStatus.EXPIRED
        || status == BillingStatus.CANCELLED
        || status == BillingStatus.REFUNDED) {
      throw new InvalidBillingStateChangeException("만료된 청구는 결제 완료할 수 없습니다.");
    }
    this.status = BillingStatus.PAID;
  }
}
