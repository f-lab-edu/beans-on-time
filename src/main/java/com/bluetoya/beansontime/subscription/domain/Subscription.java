package com.bluetoya.beansontime.subscription.domain;

public class Subscription {
    private SubscriptionId subscriptionId;
    private SubscriptionStatus subscriptionStatus;

    private CustomerId customerId;
    private ProductId productId;

    private Cycle cycle;

    public Subscription(CustomerId customerId, ProductId productId, Cycle cycle) {
        this.customerId = customerId;
        this.productId = productId;
        this.cycle = cycle;
    }
}
