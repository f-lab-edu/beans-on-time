package com.bluetoya.beansontime.billing.domain.exception;

public class InvalidBillingStateChangeException extends RuntimeException {
  public InvalidBillingStateChangeException(String message) {
    super(message);
  }
}
