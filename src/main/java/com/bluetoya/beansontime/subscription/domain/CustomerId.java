package com.bluetoya.beansontime.subscription.domain;

public record CustomerId(long id) {

  public CustomerId {
    if (id < 1) {
      throw new IllegalArgumentException("CustomerId must be greater than 0");
    }
  }
}
