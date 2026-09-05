package com.bluetoya.beansontime.billing.domain;

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
  private BillingStatus status;

  public Billing(
      CustomerId customerId,
      SubscriptionId subscriptionId,
      ProductId productId,
      Money amount,
      LocalDate billingDate,
      LocalDateTime createdAt) {
    this.id = BillingId.generate();
    this.customerId = Objects.requireNonNull(customerId, "청구 고객 ID는 필수입니다.");
    this.subscriptionId = Objects.requireNonNull(subscriptionId, "청구 구독 ID는 필수입니다.");
    this.productId = Objects.requireNonNull(productId, "청구 상품 ID는 필수입니다.");
    this.amount = Objects.requireNonNull(amount, "청구 금액은 필수입니다.");
    this.billingDate = Objects.requireNonNull(billingDate, "청구일은 필수입니다.");
    this.createdAt = Objects.requireNonNull(createdAt, "청구 생성 시각은 필수입니다.");
    this.status = BillingStatus.PENDING;
  }

  public void markPaid() {
    this.status = BillingStatus.PAID;
  }
}
