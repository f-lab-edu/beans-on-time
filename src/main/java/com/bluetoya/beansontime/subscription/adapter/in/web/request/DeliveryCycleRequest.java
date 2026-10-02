package com.bluetoya.beansontime.subscription.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record DeliveryCycleRequest(
    @NotNull(message = "납품 주기 단위는 필수입니다.")
        @Pattern(regexp = "ONE_WEEK|ONE_MONTH", message = "유효하지 않은 납품 주기 단위입니다.")
        String unit,
    @Positive(message = "납품 주기 간격은 0보다 커야 합니다.") int interval) {}
