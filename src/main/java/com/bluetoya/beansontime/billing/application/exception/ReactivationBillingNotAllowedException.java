package com.bluetoya.beansontime.billing.application.exception;

public class ReactivationBillingNotAllowedException extends RuntimeException {
  public ReactivationBillingNotAllowedException(String message) {
    super(message);
  }
}
