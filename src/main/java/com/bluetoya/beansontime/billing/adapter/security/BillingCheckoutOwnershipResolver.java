package com.bluetoya.beansontime.billing.adapter.security;

import com.bluetoya.beansontime.billing.application.port.in.BillingCheckoutDetail;
import com.bluetoya.beansontime.security.authorization.OwnershipResolver;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import org.springframework.stereotype.Component;

@Component
public class BillingCheckoutOwnershipResolver implements OwnershipResolver<BillingCheckoutDetail> {

  @Override
  public Class<BillingCheckoutDetail> targetType() {
    return BillingCheckoutDetail.class;
  }

  @Override
  public ActorIdentity resolveOwner(BillingCheckoutDetail checkout) {
    return new ActorIdentity(ActorType.CUSTOMER, checkout.customerId());
  }
}
