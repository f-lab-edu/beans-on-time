package com.bluetoya.beansontime.subscription.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Cycle {
    private CycleId cycleId;
    private CycleUnit unit;
    private int interval;

    public Cycle(CycleUnit unit, int interval) {
    }
}
