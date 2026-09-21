package com.bluetoya.beansontime.product.adapter.in.web.response;

public record ProductDetailResponse(
    long productId, String name, String description, int basePrice, String status) {}
