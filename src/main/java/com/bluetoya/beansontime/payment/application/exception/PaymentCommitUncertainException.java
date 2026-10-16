package com.bluetoya.beansontime.payment.application.exception;

public class PaymentCommitUncertainException extends RuntimeException {
  public PaymentCommitUncertainException(Throwable cause) {
    super("결제 결과의 DB 반영 여부를 다시 확인해야 합니다.", cause);
  }
}
