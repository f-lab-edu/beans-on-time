package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import java.util.Optional;

public interface FindGatewayPaymentPort {
  Optional<PaymentGatewayResult> find(PaymentId paymentId);
}
