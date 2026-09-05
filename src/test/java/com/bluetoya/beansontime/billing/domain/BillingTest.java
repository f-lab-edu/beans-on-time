package com.bluetoya.beansontime.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BillingTest {

  @Test
  void createsAPendingBillingWithAnImmutablePriceSnapshot() {
    Money productPriceAtPreparation = new Money(30000);
    Billing billing = billing(productPriceAtPreparation);
    Money changedProductPrice = new Money(35000);

    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PENDING);
    assertThat(billing.getAmount()).isEqualTo(new Money(30000));
    assertThat(changedProductPrice).isEqualTo(new Money(35000));
  }

  @Test
  void marksAPendingBillingAsPaidAndKeepsRepeatedCompletionIdempotent() {
    Billing billing = billing(new Money(30000));

    billing.markPaid();
    billing.markPaid();

    assertThat(billing.getStatus()).isEqualTo(BillingStatus.PAID);
  }

  @Test
  void requiresEveryBillingFactIncludingTheAmount() {
    SubscriptionId subscriptionId = SubscriptionId.generate();
    ProductId productId = new ProductId(10);
    Money amount = new Money(30000);
    LocalDate billingDate = LocalDate.of(2026, 9, 2);
    LocalDateTime createdAt = LocalDateTime.of(2026, 9, 2, 10, 0);

    assertThatNullPointerException()
        .isThrownBy(
            () -> new Billing(null, subscriptionId, productId, amount, billingDate, createdAt));
    assertThatNullPointerException()
        .isThrownBy(
            () -> new Billing(new CustomerId(1), null, productId, amount, billingDate, createdAt));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Billing(
                    new CustomerId(1), subscriptionId, null, amount, billingDate, createdAt));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new Billing(
                    new CustomerId(1), subscriptionId, productId, null, billingDate, createdAt));
  }

  private Billing billing(Money amount) {
    return new Billing(
        new CustomerId(1),
        SubscriptionId.generate(),
        new ProductId(10),
        amount,
        LocalDate.of(2026, 9, 2),
        LocalDateTime.of(2026, 9, 2, 10, 0));
  }
}
