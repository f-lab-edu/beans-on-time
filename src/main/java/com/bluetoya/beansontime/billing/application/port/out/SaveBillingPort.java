package com.bluetoya.beansontime.billing.application.port.out;

import com.bluetoya.beansontime.billing.domain.Billing;

public interface SaveBillingPort {
  void save(Billing billing);
}
