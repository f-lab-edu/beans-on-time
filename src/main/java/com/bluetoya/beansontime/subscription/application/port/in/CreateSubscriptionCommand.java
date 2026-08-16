package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.Cycle;
import com.bluetoya.beansontime.subscription.domain.ProductId;

public record CreateSubscriptionCommand(CustomerId customerId, ProductId productId, Cycle cycle) {}
