package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;

public interface FindSubscriptionQueryPort {
  SubscriptionQueryResult find(SubscriptionId subscriptionId);
}
