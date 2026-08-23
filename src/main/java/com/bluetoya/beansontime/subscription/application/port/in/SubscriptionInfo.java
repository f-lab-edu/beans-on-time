package com.bluetoya.beansontime.subscription.application.port.in;

public record SubscriptionInfo(
        String subscriptionId,
        long customerId,
        String cycleUnit,
        int cycleInterval,
        String status
) {
}
