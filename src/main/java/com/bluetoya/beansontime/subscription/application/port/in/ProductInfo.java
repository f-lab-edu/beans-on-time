package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.product.domain.ProductId;

public record ProductInfo(
        ProductAvailability availability,
        long productId,
        String name,
        int basePrice
) {
    public static ProductInfo toUnavailableProductInfo(ProductId productId) {
        return new ProductInfo(
                ProductAvailability.UNAVAILABLE,
                productId.id(),
                null,
                0
        );
    }
}