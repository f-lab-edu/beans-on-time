package com.bluetoya.beansontime.subscription.adapter.in.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

public record SubscribeRequest(
    @Positive(message = "상품 ID는 0보다 커야 합니다.") long productId,
    @Valid DeliveryCycleRequest deliveryCycle) {}
