package com.bluetoya.beansontime.product.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ProductStatus {
    ACTIVE("판매중"),
    INACTIVE("판매중지"),
    ;

    private final String description;
}
