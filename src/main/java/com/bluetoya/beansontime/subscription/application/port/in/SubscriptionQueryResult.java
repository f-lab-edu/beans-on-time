package com.bluetoya.beansontime.subscription.application.port.in;

import java.util.UUID;

public record SubscriptionQueryResult(
    UUID subscriptionId,
    long customerId,
    long productId,
    String cycleUnit,
    int cycleInterval,
    String status) {}
