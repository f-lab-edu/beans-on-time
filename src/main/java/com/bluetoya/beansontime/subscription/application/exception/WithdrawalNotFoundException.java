package com.bluetoya.beansontime.subscription.application.exception;

public class WithdrawalNotFoundException extends RuntimeException {
  public WithdrawalNotFoundException(String message) {
    super(message);
  }
}
