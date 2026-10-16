package com.bluetoya.beansontime.payment.application.exception;

public class PaymentCommitRetryableException extends PaymentCommitUncertainException {
  public PaymentCommitRetryableException(Throwable cause) {
    super(cause);
  }
}
