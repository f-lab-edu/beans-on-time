package com.bluetoya.beansontime.payment.application.port.in;

import com.bluetoya.beansontime.billing.domain.BillingId;

public interface PreparePaymentCheckoutUseCase {
  PaymentCheckoutDetail prepare(BillingId billingId);
}
