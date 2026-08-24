package com.bluetoya.beansontime.subscription.adapter.security;

import com.bluetoya.beansontime.security.authorization.OwnershipResolver;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionOwnershipResolver implements OwnershipResolver<Subscription> {

  @Override
  public Class<Subscription> targetType() {
    return Subscription.class;
  }

  @Override
  public ActorIdentity resolveOwner(Subscription subscription) {
    return new ActorIdentity(ActorType.CUSTOMER, subscription.getCustomerId().value());
  }
}
