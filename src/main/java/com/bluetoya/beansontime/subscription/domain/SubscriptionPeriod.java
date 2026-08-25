package com.bluetoya.beansontime.subscription.domain;

import java.time.LocalDate;
import java.util.Objects;

public record SubscriptionPeriod(LocalDate startDate, LocalDate endDate) {

  public SubscriptionPeriod {
    Objects.requireNonNull(startDate, "구독 회차 시작일은 필수입니다.");
    Objects.requireNonNull(endDate, "구독 회차 종료일은 필수입니다.");

    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("구독 회차 종료일은 시작일보다 빠를 수 없습니다.");
    }
  }
}
