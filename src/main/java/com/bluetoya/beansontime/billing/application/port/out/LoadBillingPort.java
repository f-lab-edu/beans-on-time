package com.bluetoya.beansontime.billing.application.port.out;

import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingId;
import java.util.Optional;

public interface LoadBillingPort {
  Optional<Billing> load(BillingId billingId);
}
