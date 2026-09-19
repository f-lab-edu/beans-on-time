package com.bluetoya.beansontime.product.domain.exception;

public class InvalidSupplyStateChangeException extends RuntimeException {
  public InvalidSupplyStateChangeException(String message) {
    super(message);
  }
}
