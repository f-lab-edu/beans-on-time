package com.bluetoya.beansontime.subscription.adapter.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubscriptionTimeConfig {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  @Bean
  Clock businessClock() {
    return Clock.system(KST);
  }
}
