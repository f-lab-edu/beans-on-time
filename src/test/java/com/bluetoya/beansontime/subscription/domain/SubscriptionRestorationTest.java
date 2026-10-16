package com.bluetoya.beansontime.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPeriodStateException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubscriptionRestorationTest {
  private final LocalDate start = LocalDate.of(2026, 1, 31);
  private final LocalDate nextBillingDate = LocalDate.of(2026, 2, 28);
  private final SubscriptionPeriod period =
      new SubscriptionPeriod(start, nextBillingDate.minusDays(1));

  @Test
  void preservesIdentityAndCopiesSuspensionReasons() {
    var id = SubscriptionId.generate();
    var reasons = EnumSet.of(SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE);
    var restored =
        Subscription.restore(
            id,
            new CustomerId(1),
            new ProductId(1),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            start,
            new BillingAnchorDay(31),
            period,
            null,
            nextBillingDate,
            null,
            null,
            SubscriptionStatus.ACTIVE,
            reasons);
    reasons.clear();
    assertThat(restored.getId()).isEqualTo(id);
    assertThat(restored.getBillingAnchorDay().value()).isEqualTo(31);
    assertThat(restored.getSuspensionReasons())
        .containsExactly(SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE);
  }

  @Test
  void rejectsActiveSubscriptionWithoutPaidPeriod() {
    assertThatThrownBy(
            () -> restore(SubscriptionStatus.ACTIVE, null, null, nextBillingDate, null, null))
        .isInstanceOf(InvalidSubscriptionPeriodStateException.class);
  }

  @Test
  void rejectsActiveSubscriptionWithMismatchedBillingDate() {
    assertThatThrownBy(
            () ->
                restore(
                    SubscriptionStatus.ACTIVE,
                    period,
                    null,
                    nextBillingDate.plusDays(1),
                    null,
                    null))
        .isInstanceOf(InvalidSubscriptionPeriodStateException.class);
  }

  @Test
  void rejectsPausedSubscriptionWithNegativeDaysOrInvalidSchedule() {
    var pausedAt = LocalDateTime.of(2026, 2, 10, 10, 0);
    var scheduled = LocalDate.of(2026, 2, 20);
    assertThatThrownBy(
            () ->
                restore(
                    SubscriptionStatus.PAUSED,
                    null,
                    -1,
                    scheduled.minusDays(1),
                    pausedAt,
                    scheduled))
        .isInstanceOf(InvalidSubscriptionPeriodStateException.class);
    assertThatThrownBy(
            () ->
                restore(
                    SubscriptionStatus.PAUSED, null, 5, scheduled.plusDays(6), pausedAt, scheduled))
        .isInstanceOf(InvalidSubscriptionPeriodStateException.class);
    assertThatThrownBy(
            () ->
                restore(
                    SubscriptionStatus.PAUSED,
                    null,
                    0,
                    pausedAt.toLocalDate(),
                    pausedAt,
                    pausedAt.toLocalDate()))
        .isInstanceOf(InvalidSubscriptionPeriodStateException.class);
  }

  @Test
  void acceptsPausedSubscriptionWithZeroRemainingDaysAndRejectsCancelledPeriod() {
    var scheduled = LocalDate.of(2026, 3, 1);
    var paused =
        restore(
            SubscriptionStatus.PAUSED,
            null,
            0,
            scheduled,
            LocalDateTime.of(2026, 2, 27, 10, 0),
            scheduled);
    assertThat(paused.isPaidReactivationTarget()).isTrue();
    assertThatThrownBy(() -> restore(SubscriptionStatus.CANCELLED, period, null, null, null, null))
        .isInstanceOf(InvalidSubscriptionPeriodStateException.class);
  }

  private Subscription restore(
      SubscriptionStatus status,
      SubscriptionPeriod currentPeriod,
      Integer remainingDays,
      LocalDate nextDate,
      LocalDateTime pausedAt,
      LocalDate scheduled) {
    return Subscription.restore(
        SubscriptionId.generate(),
        new CustomerId(1),
        new ProductId(1),
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        start,
        new BillingAnchorDay(31),
        currentPeriod,
        remainingDays,
        nextDate,
        pausedAt,
        scheduled,
        status,
        Set.of());
  }
}
