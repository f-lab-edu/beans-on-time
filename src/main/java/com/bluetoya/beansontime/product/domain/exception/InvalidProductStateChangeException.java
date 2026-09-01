package com.bluetoya.beansontime.product.domain.exception;

public class InvalidProductStateChangeException extends RuntimeException {
  public InvalidProductStateChangeException(String message) {
    super(message);
  }
}
