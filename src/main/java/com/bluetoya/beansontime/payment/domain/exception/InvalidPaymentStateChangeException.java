package com.bluetoya.beansontime.payment.domain.exception;

public class InvalidPaymentStateChangeException extends RuntimeException {
  public InvalidPaymentStateChangeException(String message) {
    super(message);
  }
}
