package com.bluetoya.beansontime.product.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@Getter
@Builder
@RequiredArgsConstructor
public class Product {
    private final ProductId id;
    private final SellerId sellerId;

    private final String name;
    private final String description;
    private final Money basePrice;

    private final List<ProductImage> images;
    private final List<ProductSizeOption> sizeOptions;
    private final Set<GrindType> grindTypes;

    private final ProductStatus status;

    public record ProductImage(String url, int order) {
    }

    public record ProductSizeOption(int gramSize, Money additionalPrice) {
    }

}
