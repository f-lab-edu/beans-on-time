package com.bluetoya.beansontime.payment.adapter.out.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayRequest;
import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import com.bluetoya.beansontime.product.domain.Money;
import org.junit.jupiter.api.Test;

class FakePaymentGatewayAdapterTest {

  private static final PaymentGatewayRequest REQUEST =
      new PaymentGatewayRequest(new BillingId(1), new Money(30000));

  @Test
  void allowsApprovalAndDeclineResultsToBeControlled() {
    FakePaymentGatewayAdapter gateway = new FakePaymentGatewayAdapter();
    gateway.approveNext("transaction-1");

    PaymentGatewayResult approved = gateway.pay(REQUEST);
    gateway.declineNext();
    PaymentGatewayResult declined = gateway.pay(REQUEST);

    assertThat(approved.successful()).isTrue();
    assertThat(approved.transactionId()).isEqualTo("transaction-1");
    assertThat(declined.successful()).isFalse();
    assertThat(declined.transactionId()).isNull();
  }

  @Test
  void representsGatewayUnavailabilityAsAnExceptionInsteadOfADecline() {
    FakePaymentGatewayAdapter gateway = new FakePaymentGatewayAdapter();
    gateway.failNext();

    assertThatThrownBy(() -> gateway.pay(REQUEST))
        .isInstanceOf(PaymentGatewayUnavailableException.class);
    assertThat(gateway.pay(REQUEST).successful()).isTrue();
  }
}
