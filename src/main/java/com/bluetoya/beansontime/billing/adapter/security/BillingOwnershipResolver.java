package com.bluetoya.beansontime.billing.adapter.security;

import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.security.authorization.OwnershipResolver;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import org.springframework.stereotype.Component;

@Component
public class BillingOwnershipResolver implements OwnershipResolver<Billing> {

  @Override
  public Class<Billing> targetType() {
    return Billing.class;
  }

  @Override
  public ActorIdentity resolveOwner(Billing billing) {
    return new ActorIdentity(ActorType.CUSTOMER, billing.getCustomerId().value());
  }
}
