package com.bluetoya.beansontime.subscription.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Subscription {
    private final SubscriptionId subscriptionId;
    private final CustomerId customerId;
    private final ProductId productId;
    private Cycle cycle;
    private SubscriptionStatus subscriptionStatus;

    public Subscription(CustomerId customerId, ProductId productId, Cycle cycle) {
        this.subscriptionId = SubscriptionId.generate();
        this.customerId = customerId;
        this.productId = productId;
        this.cycle = cycle;
        this.subscriptionStatus = SubscriptionStatus.ACTIVE;
    }
}
