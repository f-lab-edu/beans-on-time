package com.bluetoya.beansontime.subscription.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SubscriptionStatus {
  ACTIVE("활성화"),
  PAUSED("일시중지"),
  CANCEL("취소"),
  ;

  private final String description;
}
