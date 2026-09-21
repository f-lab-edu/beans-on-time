package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.billing.domain.BillingId;

public interface ExistsPaymentPort {
  boolean existsByBillingId(BillingId billingId);
}
