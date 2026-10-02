package com.bluetoya.beansontime.payment.application.port.in;

public interface ReconcilePaymentUseCase {
  PaymentResult reconcile(ReconcilePaymentCommand command);
}
