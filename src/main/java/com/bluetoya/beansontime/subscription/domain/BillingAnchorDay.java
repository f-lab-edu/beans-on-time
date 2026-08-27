package com.bluetoya.beansontime.subscription.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public record BillingAnchorDay(int value) {

  public BillingAnchorDay {
    if (value < 1 || value > 31) {
      throw new IllegalArgumentException("결제 기준일은 1일부터 31일 사이여야 합니다.");
    }
  }

  public static BillingAnchorDay from(LocalDate date) {
    Objects.requireNonNull(date, "기준 날짜는 필수입니다.");
    return new BillingAnchorDay(date.getDayOfMonth());
  }

  public LocalDate nextBillingDateAfter(LocalDate date) {
    Objects.requireNonNull(date, "기준 날짜는 필수입니다.");
    YearMonth currentMonth = YearMonth.from(date);
    LocalDate candidate = billingDateIn(currentMonth);

    return candidate.isAfter(date) ? candidate : billingDateIn(currentMonth.plusMonths(1));
  }

  public LocalDate billingDateIn(YearMonth targetMonth) {
    Objects.requireNonNull(targetMonth, "대상 연월은 필수입니다.");
    return targetMonth.atDay(Math.min(value, targetMonth.lengthOfMonth()));
  }
}
