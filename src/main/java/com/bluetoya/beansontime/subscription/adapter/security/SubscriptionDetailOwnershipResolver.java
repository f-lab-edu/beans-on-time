package com.bluetoya.beansontime.subscription.adapter.security;

import com.bluetoya.beansontime.security.authorization.OwnershipResolver;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionDetail;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionDetailOwnershipResolver implements OwnershipResolver<SubscriptionDetail> {
    @Override
    public Class<SubscriptionDetail> targetType() {
        return SubscriptionDetail.class;
    }

    @Override
    public ActorIdentity resolveOwner(SubscriptionDetail result) {
        return new ActorIdentity(
                ActorType.CUSTOMER,
                result.subscriptionInfo().customerId()
        );
    }
}
