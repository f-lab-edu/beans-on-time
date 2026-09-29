package com.bluetoya.beansontime.payment.application.exception;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import lombok.Getter;

@Getter
public class PaymentInProgressException extends RuntimeException {
  private final PaymentId paymentId;

  public PaymentInProgressException(PaymentId paymentId) {
    super("결제 결과를 확인 중입니다. 기존 결제 결과를 조회해 주세요.");
    this.paymentId = paymentId;
  }
}
