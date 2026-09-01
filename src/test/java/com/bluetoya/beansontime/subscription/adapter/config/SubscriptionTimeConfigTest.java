package com.bluetoya.beansontime.subscription.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SubscriptionTimeConfigTest {

  @Test
  void providesKstBusinessClock() {
    Clock clock = new SubscriptionTimeConfig().businessClock();

    assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
  }
}
