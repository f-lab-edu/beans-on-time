package com.bluetoya.beansontime.billing.application.exception;

public class BillingAlreadyPaidException extends RuntimeException {
  public BillingAlreadyPaidException(String message) {
    super(message);
  }
}
