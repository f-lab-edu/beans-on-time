package com.bluetoya.beansontime.billing.application.port.in;

import com.bluetoya.beansontime.billing.domain.BillingId;

public interface GetBillingCheckoutQuery {
  BillingCheckoutDetail find(BillingId billingId);
}
