package com.bluetoya.beansontime.subscription.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DeliveryCycleUnit {
  ONE_WEEK("1주"),
  ONE_MONTH("1달"),
  ;

  private final String description;

  public static DeliveryCycleUnit of(String unit) {
    return switch (unit) {
      case "1주" -> ONE_WEEK;
      case "1달" -> ONE_MONTH;
      default -> throw new IllegalArgumentException("유효하지 않은 배송 주기 단위입니다.");
    };
  }
}
