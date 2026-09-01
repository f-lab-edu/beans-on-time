package com.bluetoya.beansontime.subscription.domain;

import lombok.Getter;

@Getter
public class DeliveryCycle {
  private final DeliveryCycleUnit unit;
  private final int interval;

  public DeliveryCycle(DeliveryCycleUnit unit, int interval) {
    this.unit = unit;
    this.interval = interval;
  }
}
