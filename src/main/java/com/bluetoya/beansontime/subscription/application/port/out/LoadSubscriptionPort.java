package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;

import java.util.Optional;

public interface LoadSubscriptionPort {
    Optional<Subscription> load(SubscriptionId subscriptionId);
}
