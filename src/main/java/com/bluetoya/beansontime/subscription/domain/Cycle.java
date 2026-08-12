package com.bluetoya.beansontime.subscription.domain;

import lombok.Getter;

@Getter
public class Cycle {
    private final CycleUnit unit;
    private final int interval;

    public Cycle(CycleUnit unit, int interval) {
        this.unit = unit;
        this.interval = interval;
    }
}
