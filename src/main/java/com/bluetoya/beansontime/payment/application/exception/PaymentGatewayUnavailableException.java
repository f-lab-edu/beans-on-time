package com.bluetoya.beansontime.payment.application.exception;

public class PaymentGatewayUnavailableException extends RuntimeException {
  public PaymentGatewayUnavailableException(String message) {
    super(message);
  }
}
