package com.bluetoya.beansontime.billing.application.exception;

public class BillingExpiredException extends RuntimeException {
  public BillingExpiredException(String message) {
    super(message);
  }
}
