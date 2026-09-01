package com.bluetoya.beansontime.subscription.domain.exception;

public class InvalidSubscriptionPeriodStateException extends IllegalStateException {
  public InvalidSubscriptionPeriodStateException(String message) {
    super(message);
  }
}
