package com.bluetoya.beansontime.subscription.adapter.security;

import com.bluetoya.beansontime.security.authorization.OwnershipResolver;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionQueryResultOwnershipResolver implements OwnershipResolver<SubscriptionQueryResult> {
    @Override
    public Class<SubscriptionQueryResult> targetType() {
        return SubscriptionQueryResult.class;
    }

    @Override
    public ActorIdentity resolveOwner(SubscriptionQueryResult result) {
        return new ActorIdentity(
                ActorType.CUSTOMER,
                result.customerId()
        );
    }
}
