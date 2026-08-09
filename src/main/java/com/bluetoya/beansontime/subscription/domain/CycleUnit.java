package com.bluetoya.beansontime.subscription.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CycleUnit {
    ONE_WEEK("1주"),
    ONE_MONTH("1달"),
    ;

    private final String description;

}
