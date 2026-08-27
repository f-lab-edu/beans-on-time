package com.bluetoya.beansontime.subscription.domain.exception;

public class InvalidSubscriptionResumeDateException extends IllegalStateException {
  public InvalidSubscriptionResumeDateException(String message) {
    super(message);
  }
}
