package com.bluetoya.beansontime.billing.application.port.out;

import com.bluetoya.beansontime.billing.application.port.in.BillingCheckoutDetail;
import com.bluetoya.beansontime.billing.domain.BillingId;

public interface GetBillingCheckoutQueryPort {
  BillingCheckoutDetail get(BillingId billingId);
}
