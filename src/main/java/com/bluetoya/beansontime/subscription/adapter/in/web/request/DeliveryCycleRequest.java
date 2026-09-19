package com.bluetoya.beansontime.subscription.adapter.in.web.request;

import jakarta.validation.constraints.Positive;

public record DeliveryCycleRequest(String unit, @Positive(message = "배송 주기 간격은 0보다 커야 합니다.") int interval) {}
