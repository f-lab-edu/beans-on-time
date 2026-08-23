package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.Cycle;

public record SubscribeCommand(ProductId productId, Cycle cycle) {}
