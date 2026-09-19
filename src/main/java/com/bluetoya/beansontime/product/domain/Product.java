package com.bluetoya.beansontime.product.domain;

import com.bluetoya.beansontime.seller.domain.SellerId;

import java.util.List;
import java.util.Set;

public record Product(ProductId id, SellerId sellerId, String name, String description, Money basePrice,
                      List<ProductImage> images, List<ProductSizeOption> sizeOptions, Set<GrindType> grindTypes,
                      ProductStatus status) {

  public Product(SellerId sellerId, String name, Money basePrice) {
        this(ProductId.generate(), sellerId, name, "", basePrice, List.of(), List.of(), Set.of(), ProductStatus.ACTIVE);
    }

    public record ProductImage(String url, int order) {
    }

    public record ProductSizeOption(int gramSize, Money additionalPrice) {
    }
}
