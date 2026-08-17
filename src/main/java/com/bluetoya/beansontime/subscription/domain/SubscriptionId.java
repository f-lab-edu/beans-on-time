package com.bluetoya.beansontime.subscription.domain;

import java.util.UUID;

public record SubscriptionId(UUID value) {

    static SubscriptionId generate() {
        return new SubscriptionId(UUID.randomUUID());
    }
}
