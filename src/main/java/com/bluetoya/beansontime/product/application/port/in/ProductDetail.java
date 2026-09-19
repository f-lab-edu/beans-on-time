package com.bluetoya.beansontime.product.application.port.in;

public record ProductDetail(
    long productId, String name, String description, int basePrice, String status) {}
