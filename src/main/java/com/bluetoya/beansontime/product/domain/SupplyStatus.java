package com.bluetoya.beansontime.product.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SupplyStatus {
  AVAILABLE("공급 가능"),
  TEMPORARILY_UNAVAILABLE("일시 공급 불가"),
  DISCONTINUED("공급 종료"),
  ;

  private final String description;
}
