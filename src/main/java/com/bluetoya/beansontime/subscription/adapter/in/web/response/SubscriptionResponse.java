package com.bluetoya.beansontime.subscription.adapter.in.web.response;

import java.util.UUID;

public record SubscriptionResponse(UUID subscriptionId,
                                   long customerId,
                                   long productId,
                                   CycleResponse cycle,
                                   String status) {
}
