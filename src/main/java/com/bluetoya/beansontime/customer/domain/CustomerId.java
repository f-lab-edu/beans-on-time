package com.bluetoya.beansontime.customer.domain;

public record CustomerId(long value) {

  public CustomerId {
    if (value < 1) {
      throw new IllegalArgumentException("고객 ID는 0보다 커야 합니다.");
    }
  }
}
