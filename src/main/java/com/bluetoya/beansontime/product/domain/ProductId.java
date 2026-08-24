package com.bluetoya.beansontime.product.domain;

public record ProductId(long id) {

  public ProductId {
    if (id < 1) {
      throw new IllegalArgumentException("ProductId must be greater than 0");
    }
  }

  static ProductId generate() {
    return new ProductId((long) (Math.random() * 1000000000));
  }
}
