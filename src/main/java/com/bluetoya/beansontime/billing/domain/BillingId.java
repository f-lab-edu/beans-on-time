package com.bluetoya.beansontime.billing.domain;

import java.util.concurrent.ThreadLocalRandom;

public record BillingId(long value) {

  public BillingId {
    if (value < 1) {
      throw new IllegalArgumentException("청구 ID는 0보다 커야 합니다.");
    }
  }

  public static BillingId generate() {
    return new BillingId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
  }
}
