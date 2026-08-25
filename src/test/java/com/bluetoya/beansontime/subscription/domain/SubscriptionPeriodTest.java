package com.bluetoya.beansontime.subscription.domain;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SubscriptionPeriodTest {

  @Test
  void endDateCannotBeBeforeStartDate() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new SubscriptionPeriod(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 29)));
  }
}
