package com.bluetoya.beansontime.product.application.port.in;

import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.SellerId;

public record RegisterProductCommand(SellerId sellerId, String name, Money basePrice) {
}
