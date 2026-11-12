package com.bluetoya.beansontime.subscription.domain;

import static org.assertj.core.api.Assertions.*;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.exception.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RecurringSubscriptionTest {
  private Subscription subscription(LocalDate started) {
    return new Subscription(
        new CustomerId(1),
        new ProductId(1),
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        started);
  }

  @ParameterizedTest
  @ValueSource(ints = {2026, 2028})
  void delayedFebruaryRenewalPreservesThirtyFirstAnchor(int year) {
    var subscription = subscription(LocalDate.of(year, 1, 31));
    var due = subscription.getNextBillingDate();
    subscription.addSuspensionReason(SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE);
    subscription.renewAfterPayment(due, due.plusDays(2));
    assertThat(subscription.getCurrentPeriod().startDate()).isEqualTo(due);
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(year, 3, 31));
    assertThat(subscription.getBillingAnchorDay().value()).isEqualTo(31);
    assertThat(subscription.getSuspensionReasons())
        .containsExactly(SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE);
  }

  @Test
  void rejectsOldPeriodAndApprovalBeforeDueDate() {
    var subscription = subscription(LocalDate.of(2026, 9, 4));
    var due = subscription.getNextBillingDate();
    assertThatThrownBy(() -> subscription.renewAfterPayment(due, due.minusDays(1)))
        .isInstanceOf(InvalidSubscriptionPaymentDateException.class);
    subscription.renewAfterPayment(due, due);
    assertThatThrownBy(() -> subscription.renewAfterPayment(due, due))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);
  }

  @Test
  void cancelledSubscriptionCannotBeRenewedByLateApproval() {
    var subscription = subscription(LocalDate.of(2026, 9, 4));
    var due = subscription.getNextBillingDate();
    subscription.cancel();
    assertThatThrownBy(() -> subscription.renewAfterPayment(due, due))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);
    subscription.recordRecurringPaymentDeclined(due);
    assertThat(subscription.getSuspensionReasons()).isEmpty();
  }
}
