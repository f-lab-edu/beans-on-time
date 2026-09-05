package com.bluetoya.beansontime.payment.application.exception;

public class PaymentAlreadyAttemptedException extends RuntimeException {
  public PaymentAlreadyAttemptedException(String message) {
    super(message);
  }
}
