package com.bluetoya.beansontime.security.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ActorType {
    CUSTOMER("고객"),
    SELLER("판매자"),
    ;

    private final String description;
}
