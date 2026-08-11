package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.SubscriptionId;

public interface GetSubscriptionQuery {
    SubscriptionQueryResult get(SubscriptionId subscriptionId);
}
