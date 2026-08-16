package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;

public record PauseSubscriptionCommand(SubscriptionId subscriptionId, CustomerId customerId) {
}
