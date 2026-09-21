package com.bluetoya.beansontime.payment.application.port.in;

public interface PayBillingUseCase {
  PaymentResult pay(PayBillingCommand command);
}
