package com.bluetoya.beansontime.product.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GrindType {
    WHOLE_BEAN("홀빈", "직접 분쇄"),
    COLD_BREW("콜드브루", "매우 굵게"),
    FRENCH_PRESS("프렌치프레스", "굵게"),
    POUR_OVER("핸드드립", "중간"),
    MOKA_POT("모카 포트", "곱게"),
    ESPRESSO("에스프레소", "매우 곱게"),
    ;

    private final String description;
    private final String additionalInfo;
}
