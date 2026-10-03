package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.payment.application.port.out.PaymentGatewayResult;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentId;

public interface PaymentCompletion {
  Payment complete(PaymentId id, PaymentGatewayResult result);

  default boolean recover(PaymentId id) {
    return false;
  }
}
