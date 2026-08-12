package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.SubscriptionId;

public interface FindSubscriptionQuery {
    SubscriptionQueryResult find(SubscriptionId subscriptionId);
}
