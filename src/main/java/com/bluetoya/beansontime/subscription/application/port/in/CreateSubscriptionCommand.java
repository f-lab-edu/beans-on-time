package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.Cycle;
import com.bluetoya.beansontime.subscription.domain.ProductId;

public record CreateSubscriptionCommand(ProductId productId, Cycle cycle) {}
