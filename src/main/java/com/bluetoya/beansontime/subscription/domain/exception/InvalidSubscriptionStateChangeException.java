package com.bluetoya.beansontime.subscription.domain.exception;

public class InvalidSubscriptionStateChangeException extends RuntimeException {
  public InvalidSubscriptionStateChangeException(String message) {
    super(message);
  }
}
