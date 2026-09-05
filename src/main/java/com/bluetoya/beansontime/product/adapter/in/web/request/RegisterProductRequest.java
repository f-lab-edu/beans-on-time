package com.bluetoya.beansontime.product.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RegisterProductRequest(
    String name,
    @NotNull(message = "가격은 필수입니다.") @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
        Integer basePrice) {}
