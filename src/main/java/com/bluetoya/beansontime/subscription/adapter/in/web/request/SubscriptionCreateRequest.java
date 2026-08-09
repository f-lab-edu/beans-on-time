package com.bluetoya.beansontime.subscription.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscriptionCreateRequest(
        @Positive long customerId,
        @Positive long productId,
        @NotNull CycleRequest cycle) {
}
