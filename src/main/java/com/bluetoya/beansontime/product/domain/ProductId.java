package com.bluetoya.beansontime.product.domain;

import java.util.concurrent.ThreadLocalRandom;

public record ProductId(long id) {
  public ProductId {
    if (id < 1) {
      throw new IllegalArgumentException("상품 ID는 0보다 커야 합니다.");
    }
  }

  static ProductId generate() {
    return new ProductId(ThreadLocalRandom.current().nextLong(1, 1000000000));
  }
}
