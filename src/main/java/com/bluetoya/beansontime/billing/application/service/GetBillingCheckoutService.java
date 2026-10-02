package com.bluetoya.beansontime.billing.application.service;

import com.bluetoya.beansontime.billing.application.port.in.BillingCheckoutDetail;
import com.bluetoya.beansontime.billing.application.port.in.GetBillingCheckoutQuery;
import com.bluetoya.beansontime.billing.application.port.out.GetBillingCheckoutQueryPort;
import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.security.annotation.RequireOwnership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBillingCheckoutService implements GetBillingCheckoutQuery {
  private final GetBillingCheckoutQueryPort getBillingCheckoutQueryPort;

  @Override
  @RequireOwnership
  public BillingCheckoutDetail find(BillingId billingId) {
    return getBillingCheckoutQueryPort.get(billingId);
  }
}
