package com.bluetoya.beansontime.billing.application.port.in;

import com.bluetoya.beansontime.billing.application.exception.BillingNotFoundException;
import com.bluetoya.beansontime.billing.application.port.out.LoadBillingPort;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.security.annotation.RequireOwnership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnedBillingLoader {
  private final LoadBillingPort loadBillingPort;

  @RequireOwnership
  public Billing load(BillingId billingId) {
    return loadBillingPort
        .load(billingId)
        .orElseThrow(() -> new BillingNotFoundException("조회할 청구가 존재하지 않습니다."));
  }
}
