package com.bluetoya.beansontime.subscription.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscribeRequest(
    @Positive(message = "상품 아이디는 0보다 커야 합니다.") long productId,
    @NotNull DeliveryCycleRequest deliveryCycle) {}
