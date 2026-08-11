package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.*;

public record SubscriptionQueryResult(
        SubscriptionId subscriptionId,
        CustomerId customerId,
        ProductId productId,
        Cycle cycle,
        SubscriptionStatus status
) {
}
