package com.bluetoya.beansontime.subscription.domain.exception;

public class SubscriptionResumeRequiresPaymentException extends RuntimeException {
  public SubscriptionResumeRequiresPaymentException(String message) {
    super(message);
  }
}
