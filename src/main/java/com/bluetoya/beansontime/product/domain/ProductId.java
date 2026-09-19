package com.bluetoya.beansontime.product.domain;

import jakarta.validation.constraints.Positive;

public record ProductId(@Positive(message = "상품 아이디는 0보다 커야 합니다.") long id) {
  static ProductId generate() {
    return new ProductId((long) (Math.random() * 1000000000));
  }
}
