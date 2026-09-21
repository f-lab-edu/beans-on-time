package com.bluetoya.beansontime.payment.application.port.out;

public interface PaymentGateway {
  PaymentGatewayResult pay(PaymentGatewayRequest request);
}
