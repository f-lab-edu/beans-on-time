package com.bluetoya.beansontime.customer.domain;

public record CustomerId(long value) {

  public CustomerId {
    if (value < 1) {
      throw new IllegalArgumentException("CustomerId must be greater than 0");
    }
  }
}
