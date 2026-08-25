package com.bluetoya.beansontime.product.domain;

public record Money(int price) {
  public Money {
    if (price < 0) {
      throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
    }
  }
}
