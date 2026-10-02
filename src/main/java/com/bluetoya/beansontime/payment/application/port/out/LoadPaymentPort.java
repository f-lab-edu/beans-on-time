package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import java.util.Optional;

public interface LoadPaymentPort {
  Optional<Payment> load(PaymentId paymentId);
}
