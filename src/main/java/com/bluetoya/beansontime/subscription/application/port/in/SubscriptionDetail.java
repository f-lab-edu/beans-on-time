package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.product.application.port.in.ProductInfo;

public record SubscriptionDetail(SubscriptionInfo subscriptionInfo, ProductInfo productInfo) {}
