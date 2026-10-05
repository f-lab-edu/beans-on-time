package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.domain.PaymentAuthorization;
import com.bluetoya.beansontime.payment.domain.PaymentId;

public interface PaymentCheckoutPort {
  String prepare(BillingId billingId);

  PaymentAuthorization load(PaymentId paymentId);
}
