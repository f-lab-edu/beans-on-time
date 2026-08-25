package com.bluetoya.beansontime.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingAnchorDayTest {

  @Test
  void restoresAnchorDayWhenCalculatingTheMonthAfterAShortMonth() {
    BillingAnchorDay anchorDay = new BillingAnchorDay(31);

    LocalDate marchBillingDate = anchorDay.nextBillingDateAfter(LocalDate.of(2026, 2, 28));

    assertThat(marchBillingDate).isEqualTo(LocalDate.of(2026, 3, 31));
  }

  @Test
  void rejectsDaysOutsideTheCalendarDayRange() {
    assertThatIllegalArgumentException().isThrownBy(() -> new BillingAnchorDay(0));
    assertThatIllegalArgumentException().isThrownBy(() -> new BillingAnchorDay(32));
  }
}
