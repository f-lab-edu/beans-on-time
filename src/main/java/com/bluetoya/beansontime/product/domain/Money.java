package com.bluetoya.beansontime.product.domain;

public record Money(int price) {
  public Money {
    if (price < 0) {
      throw new IllegalArgumentException("Price must be greater than or equal to 0");
    }
  }
}
