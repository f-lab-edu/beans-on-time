package com.bluetoya.beansontime.scheduling.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 스케줄링 기반 설정은 전역에서 활성화하고 개별 작업의 등록 여부는 각 어댑터가 결정한다. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {}
