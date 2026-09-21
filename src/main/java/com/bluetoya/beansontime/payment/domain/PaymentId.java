package com.bluetoya.beansontime.payment.domain;

import java.util.concurrent.ThreadLocalRandom;

public record PaymentId(long value) {

  public PaymentId {
    if (value < 1) {
      throw new IllegalArgumentException("결제 ID는 0보다 커야 합니다.");
    }
  }

  public static PaymentId generate() {
    return new PaymentId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
  }
}
