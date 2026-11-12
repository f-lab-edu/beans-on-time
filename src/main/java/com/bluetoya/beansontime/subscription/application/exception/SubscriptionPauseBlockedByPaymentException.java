package com.bluetoya.beansontime.subscription.application.exception;

public class SubscriptionPauseBlockedByPaymentException extends RuntimeException {
  public SubscriptionPauseBlockedByPaymentException(String message) {
    super(message);
  }
}
