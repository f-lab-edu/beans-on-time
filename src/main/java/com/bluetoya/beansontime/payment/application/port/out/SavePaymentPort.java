package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.payment.domain.Payment;

public interface SavePaymentPort {
  void save(Payment payment);
}
