package com.bluetoya.beansontime.payment.application.port.out;

public interface PaymentCommitPort {
  void commit(Runnable action);
}
