package com.bluetoya.beansontime.payment.application.port.in;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import java.util.Objects;

public record ReconcilePaymentCommand(BillingId billingId, PaymentId paymentId) {
  public ReconcilePaymentCommand {
    Objects.requireNonNull(billingId);
    Objects.requireNonNull(paymentId);
  }
}
