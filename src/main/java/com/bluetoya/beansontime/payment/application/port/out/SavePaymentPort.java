package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentAuthorization;

public interface SavePaymentPort {
  void saveNew(Payment payment);

  void save(Payment payment);

  default void saveAuthorized(Payment payment, PaymentAuthorization authorization) {
    throw new IllegalStateException("인증 결제 저장을 지원하지 않는 저장소입니다.");
  }
}
