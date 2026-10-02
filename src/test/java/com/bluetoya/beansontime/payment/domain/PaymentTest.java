package com.bluetoya.beansontime.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.domain.exception.InvalidPaymentStateChangeException;
import com.bluetoya.beansontime.product.domain.Money;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PaymentTest {

  private static final LocalDateTime ATTEMPTED_AT = LocalDateTime.of(2026, 9, 2, 10, 0);

  @Test
  void createsASuccessfulPaymentWithATransactionId() {
    Payment payment =
        Payment.succeeded(new BillingId(1), new Money(30000), "transaction-1", ATTEMPTED_AT);

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(payment.getAmount()).isEqualTo(new Money(30000));
    assertThat(payment.getTransactionId()).isEqualTo("transaction-1");
  }

  @Test
  void createsAFailedPaymentWithoutATransactionId() {
    Payment payment = Payment.failed(new BillingId(1), new Money(30000), ATTEMPTED_AT);

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(payment.getAmount()).isEqualTo(new Money(30000));
    assertThat(payment.getTransactionId()).isNull();
  }

  @Test
  void rejectsASuccessfulPaymentWithoutATransactionId() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Payment.succeeded(new BillingId(1), new Money(30000), " ", ATTEMPTED_AT));
  }

  @Test
  void startsProcessingAndKeepsConfirmedApprovalImmutable() {
    Payment payment = Payment.start(new BillingId(1), new Money(30000), ATTEMPTED_AT);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    assertThat(payment.getTransactionId()).isNull();
    payment.succeed("approval");
    payment.succeed("approval");
    assertThatThrownBy(payment::fail).isInstanceOf(InvalidPaymentStateChangeException.class);
    assertThatThrownBy(() -> payment.succeed("different"))
        .isInstanceOf(InvalidPaymentStateChangeException.class);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(payment.getTransactionId()).isEqualTo("approval");
  }

  @Test
  void keepsConfirmedDeclineImmutable() {
    Payment payment = Payment.start(new BillingId(1), new Money(30000), ATTEMPTED_AT);
    payment.fail();
    payment.fail();
    assertThatThrownBy(() -> payment.succeed("approval"))
        .isInstanceOf(InvalidPaymentStateChangeException.class);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
  }

  @Test
  void requiresTheBillingIdAndAmount() {
    assertThatNullPointerException()
        .isThrownBy(() -> Payment.failed(null, new Money(30000), ATTEMPTED_AT));
    assertThatNullPointerException()
        .isThrownBy(() -> Payment.failed(new BillingId(1), null, ATTEMPTED_AT));
  }
}
