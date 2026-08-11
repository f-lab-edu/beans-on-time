package com.bluetoya.beansontime.subscription.domain;

public record ProductId(long id) {

    public ProductId {
        if (id < 1) {
            throw new IllegalArgumentException(
                    "ProductId must be greater than 0"
            );
        }
    }
}
