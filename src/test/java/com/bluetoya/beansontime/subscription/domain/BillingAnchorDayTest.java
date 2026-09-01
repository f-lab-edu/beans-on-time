package com.bluetoya.beansontime.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingAnchorDayTest {

  @Test
  void returnsTheFirstAnchorDateAfterTheGivenDateInTheSameMonth() {
    BillingAnchorDay anchorDay = new BillingAnchorDay(31);

    LocalDate nextBillingDate = anchorDay.nextBillingDateAfter(LocalDate.of(2026, 10, 10));

    assertThat(nextBillingDate).isEqualTo(LocalDate.of(2026, 10, 31));
  }

  @Test
  void returnsTheNextMonthAnchorWhenTheCurrentMonthAnchorHasPassed() {
    BillingAnchorDay anchorDay = new BillingAnchorDay(1);

    LocalDate nextBillingDate = anchorDay.nextBillingDateAfter(LocalDate.of(2026, 10, 31));

    assertThat(nextBillingDate).isEqualTo(LocalDate.of(2026, 11, 1));
  }

  @Test
  void returnsTheNextMonthAnchorWhenTheDateIsTheCurrentAnchor() {
    BillingAnchorDay anchorDay = new BillingAnchorDay(31);

    LocalDate nextBillingDate = anchorDay.nextBillingDateAfter(LocalDate.of(2026, 10, 31));

    assertThat(nextBillingDate).isEqualTo(LocalDate.of(2026, 11, 30));
  }

  @Test
  void adjustsTheCurrentMonthAnchorToNonLeapYearFebruary() {
    BillingAnchorDay anchorDay = new BillingAnchorDay(31);

    LocalDate nextBillingDate = anchorDay.nextBillingDateAfter(LocalDate.of(2026, 2, 10));

    assertThat(nextBillingDate).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void adjustsTheCurrentMonthAnchorToLeapYearFebruary() {
    BillingAnchorDay anchorDay = new BillingAnchorDay(31);

    LocalDate nextBillingDate = anchorDay.nextBillingDateAfter(LocalDate.of(2028, 2, 10));

    assertThat(nextBillingDate).isEqualTo(LocalDate.of(2028, 2, 29));
  }

  @Test
  void adjustsTheCurrentMonthAnchorToAThirtyDayMonth() {
    BillingAnchorDay anchorDay = new BillingAnchorDay(31);

    LocalDate nextBillingDate = anchorDay.nextBillingDateAfter(LocalDate.of(2026, 4, 10));

    assertThat(nextBillingDate).isEqualTo(LocalDate.of(2026, 4, 30));
  }

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
