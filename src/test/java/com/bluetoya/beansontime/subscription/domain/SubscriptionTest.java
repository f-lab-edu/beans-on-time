package com.bluetoya.beansontime.subscription.domain;

import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.ACTIVE;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.CANCELLED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.PAUSED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PAYMENT_FAILED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPausePeriodException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPeriodStateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionResumeDateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionStateChangeException;
import com.bluetoya.beansontime.subscription.domain.exception.SubscriptionResumeRequiresPaymentException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

  @Test
  void initializesAnActivePeriodFromJanuaryThirtyFirstWithoutLosingTheAnchor() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 1, 31));

    assertThat(subscription.getStartedDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    assertThat(subscription.getDeliveryCycle())
        .usingRecursiveComparison()
        .isEqualTo(new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(31));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    assertActivePeriod(subscription, LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 27));
    assertThat(subscription.getSuspensionReasons()).isEmpty();
  }

  @Test
  void adjustsJanuaryThirtyFirstToLeapYearFebruaryWithoutLosingTheAnchor() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2028, 1, 31));

    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(31));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2028, 2, 29));
    assertActivePeriod(subscription, LocalDate.of(2028, 1, 31), LocalDate.of(2028, 2, 28));
  }

  @Test
  void pausesByReplacingTheCurrentPeriodWithRemainingPaidDays() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 4, 1));
    LocalDateTime pausedAt = LocalDateTime.of(2026, 4, 10, 14, 30);

    subscription.pause(LocalDate.of(2026, 4, 19), pausedAt);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(subscription.getCurrentPeriod()).isNull();
    assertThat(subscription.getRemainingPaidDays()).isEqualTo(20);
    assertThat(subscription.getPausedAt()).isEqualTo(pausedAt);
    assertThat(subscription.getScheduledResumeDate()).isEqualTo(LocalDate.of(2026, 4, 20));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(1));
  }

  @Test
  void allowsZeroRemainingPaidDaysWhenPausedOnTheLastPaidDay() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 7, 1));

    subscription.pause(LocalDate.of(2026, 7, 31), LocalDateTime.of(2026, 7, 31, 10, 0));

    assertThat(subscription.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(subscription.getCurrentPeriod()).isNull();
    assertThat(subscription.getRemainingPaidDays()).isZero();
    assertThat(subscription.getScheduledResumeDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 8, 1));
  }

  @Test
  void rejectsPauseUntilDateBeforePauseDateWithoutChangingState() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 4, 1));
    SubscriptionPeriod currentPeriod = subscription.getCurrentPeriod();

    assertThatThrownBy(
            () ->
                subscription.pause(LocalDate.of(2026, 4, 9), LocalDateTime.of(2026, 4, 10, 14, 30)))
        .isInstanceOf(InvalidSubscriptionPausePeriodException.class);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getCurrentPeriod()).isEqualTo(currentPeriod);
    assertThat(subscription.getRemainingPaidDays()).isNull();
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getScheduledResumeDate()).isNull();
  }

  @Test
  void rejectsAPauseDateOutsideTheCurrentActivePeriodAsAnInternalInvariantViolation() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 4, 1));

    assertThatThrownBy(
            () -> subscription.pause(LocalDate.of(2026, 5, 2), LocalDateTime.of(2026, 5, 1, 10, 0)))
        .isInstanceOf(InvalidSubscriptionPeriodStateException.class)
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void sameDayResumeDoesNotMoveTheBillingSchedule() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 4, 1));
    subscription.pause(LocalDate.of(2026, 4, 19), LocalDateTime.of(2026, 4, 10, 10, 0));

    subscription.resume(LocalDate.of(2026, 4, 10));

    assertActivePeriod(subscription, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 30));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(1));
  }

  @Test
  void nextDayResumeDoesNotMoveTheBillingSchedule() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 4, 1));
    subscription.pause(LocalDate.of(2026, 4, 19), LocalDateTime.of(2026, 4, 10, 10, 0));

    subscription.resume(LocalDate.of(2026, 4, 11));

    assertActivePeriod(subscription, LocalDate.of(2026, 4, 11), LocalDate.of(2026, 4, 30));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(1));
  }

  @Test
  void resumeAfterOneFullyFrozenDayMovesTheScheduleAndRealignsTheAnchor() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 4, 1));
    subscription.pause(LocalDate.of(2026, 4, 19), LocalDateTime.of(2026, 4, 10, 10, 0));

    subscription.resume(LocalDate.of(2026, 4, 12));

    assertActivePeriod(subscription, LocalDate.of(2026, 4, 12), LocalDate.of(2026, 5, 1));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 5, 2));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(2));
  }

  @Test
  void manualResumeBeforeTheScheduledDateUsesTheActualRequestDate() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 4, 1));
    subscription.pause(LocalDate.of(2026, 4, 30), LocalDateTime.of(2026, 4, 10, 10, 0));
    assertThat(subscription.getScheduledResumeDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 5, 21));

    subscription.resume(LocalDate.of(2026, 4, 20));

    assertActivePeriod(subscription, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 5, 9));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(10));
  }

  @Test
  void resumeOnTheScheduledDateKeepsThePlannedNextBillingDate() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 4, 1));
    subscription.pause(LocalDate.of(2026, 4, 19), LocalDateTime.of(2026, 4, 10, 10, 0));
    LocalDate plannedNextBillingDate = subscription.getNextBillingDate();

    subscription.resume(subscription.getScheduledResumeDate());

    assertThat(subscription.getNextBillingDate()).isEqualTo(plannedNextBillingDate);
    assertActivePeriod(subscription, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 5, 9));
  }

  @Test
  void manualResumeAfterTheScheduledDateUsesTheActualRequestDate() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 4, 1));
    subscription.pause(LocalDate.of(2026, 4, 19), LocalDateTime.of(2026, 4, 10, 10, 0));

    subscription.resume(LocalDate.of(2026, 4, 25));

    assertActivePeriod(subscription, LocalDate.of(2026, 4, 25), LocalDate.of(2026, 5, 14));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 5, 15));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(15));
  }

  @Test
  void oneRemainingPaidDayCreatesAValidOneDayPeriod() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2028, 2, 1));
    subscription.pause(LocalDate.of(2028, 3, 4), LocalDateTime.of(2028, 2, 28, 10, 0));
    assertThat(subscription.getRemainingPaidDays()).isEqualTo(1);

    subscription.resume(LocalDate.of(2028, 3, 5));

    assertActivePeriod(subscription, LocalDate.of(2028, 3, 5), LocalDate.of(2028, 3, 5));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2028, 3, 6));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(6));
  }

  @Test
  void zeroRemainingPaidDaysCanResumeOnTheSameDayWithoutPayment() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 7, 1));
    subscription.pause(LocalDate.of(2026, 7, 31), LocalDateTime.of(2026, 7, 31, 10, 0));

    subscription.resume(LocalDate.of(2026, 7, 31));

    assertActivePeriod(subscription, LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(1));
  }

  @Test
  void zeroRemainingPaidDaysRequiresPaymentWhenResumingAfterTheLastPaidDay() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 7, 1));
    subscription.pause(LocalDate.of(2026, 8, 10), LocalDateTime.of(2026, 7, 31, 10, 0));

    assertThatThrownBy(() -> subscription.resume(LocalDate.of(2026, 8, 1)))
        .isInstanceOf(SubscriptionResumeRequiresPaymentException.class);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(subscription.getCurrentPeriod()).isNull();
    assertThat(subscription.getRemainingPaidDays()).isZero();
    assertThat(subscription.getScheduledResumeDate()).isEqualTo(LocalDate.of(2026, 8, 11));
  }

  @Test
  void repeatedPauseAndResumePreservesTheOriginalThirtyPaidDays() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 5));

    subscription.pause(LocalDate.of(2026, 9, 19), LocalDateTime.of(2026, 9, 10, 10, 0));
    subscription.resume(LocalDate.of(2026, 9, 20));
    SubscriptionPeriod first = subscription.getCurrentPeriod();
    assertThat(first)
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 13)));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 10, 14));

    subscription.pause(LocalDate.of(2026, 10, 1), LocalDateTime.of(2026, 9, 25, 10, 0));
    subscription.resume(LocalDate.of(2026, 10, 2));
    SubscriptionPeriod second = subscription.getCurrentPeriod();
    assertThat(second)
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 19)));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 10, 20));

    subscription.pause(LocalDate.of(2026, 10, 14), LocalDateTime.of(2026, 10, 5, 10, 0));
    subscription.resume(LocalDate.of(2026, 10, 15));
    SubscriptionPeriod third = subscription.getCurrentPeriod();
    assertThat(third)
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 28)));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 10, 29));

    long paidDays =
        inclusiveDays(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 10))
            + inclusiveDays(first.startDate(), LocalDate.of(2026, 9, 25))
            + inclusiveDays(second.startDate(), LocalDate.of(2026, 10, 5))
            + inclusiveDays(third.startDate(), third.endDate());
    assertThat(paidDays).isEqualTo(30);
  }

  @Test
  void pauseAndResumeAcrossLeapYearFebruaryUsesRemainingPaidDays() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2028, 1, 31));
    subscription.pause(LocalDate.of(2028, 2, 29), LocalDateTime.of(2028, 2, 20, 10, 0));

    assertThat(subscription.getRemainingPaidDays()).isEqualTo(8);
    assertThat(subscription.getScheduledResumeDate()).isEqualTo(LocalDate.of(2028, 3, 1));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2028, 3, 9));

    subscription.resume(LocalDate.of(2028, 3, 1));

    assertActivePeriod(subscription, LocalDate.of(2028, 3, 1), LocalDate.of(2028, 3, 8));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2028, 3, 9));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(9));
  }

  @Test
  void longPauseAcrossTheYearBoundaryPreservesRemainingPaidDays() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 11, 15));
    subscription.pause(LocalDate.of(2027, 2, 4), LocalDateTime.of(2026, 11, 20, 10, 0));

    assertThat(subscription.getRemainingPaidDays()).isEqualTo(24);
    assertThat(subscription.getScheduledResumeDate()).isEqualTo(LocalDate.of(2027, 2, 5));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2027, 3, 1));

    subscription.resume(LocalDate.of(2027, 2, 5));

    assertActivePeriod(subscription, LocalDate.of(2027, 2, 5), LocalDate.of(2027, 2, 28));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2027, 3, 1));
  }

  @Test
  void rejectsResumeDateBeforePauseDateAsAnInternalTimeInvariantViolation() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    LocalDateTime pausedAt = LocalDateTime.of(2026, 9, 10, 10, 0);
    subscription.pause(LocalDate.of(2026, 9, 20), pausedAt);

    assertThatThrownBy(() -> subscription.resume(LocalDate.of(2026, 9, 9)))
        .isInstanceOf(InvalidSubscriptionResumeDateException.class)
        .isInstanceOf(IllegalStateException.class);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(subscription.getCurrentPeriod()).isNull();
    assertThat(subscription.getRemainingPaidDays()).isEqualTo(20);
    assertThat(subscription.getPausedAt()).isEqualTo(pausedAt);
    assertThat(subscription.getScheduledResumeDate()).isEqualTo(LocalDate.of(2026, 9, 21));
  }

  @Test
  void rejectsInvalidLifecycleTransitionsAndKeepsCancelIdempotent() {
    Subscription paused = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    paused.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    assertThatThrownBy(
            () -> paused.pause(LocalDate.of(2026, 10, 15), LocalDateTime.of(2026, 9, 11, 10, 0)))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);

    Subscription active = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    assertThatThrownBy(() -> active.resume(LocalDate.of(2026, 9, 20)))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);

    Subscription cancelled = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    cancelled.cancel();
    assertThatThrownBy(
            () -> cancelled.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0)))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);
    assertThatThrownBy(() -> cancelled.resume(LocalDate.of(2026, 9, 20)))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);
    cancelled.cancel();
    assertCancelledState(cancelled);
  }

  @Test
  void cancelClearsEveryDateFieldFromActiveAndPausedStates() {
    Subscription active = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    active.cancel();
    assertCancelledState(active);

    Subscription paused = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    paused.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    paused.cancel();
    assertCancelledState(paused);
  }

  @Test
  void suspensionReasonsRemainIndependentFromLifecycleTransitions() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PAYMENT_FAILED);

    subscription.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    subscription.resume(LocalDate.of(2026, 9, 20));

    assertThat(subscription.getSuspensionReasons())
        .containsExactlyInAnyOrder(PRODUCT_UNAVAILABLE, PAYMENT_FAILED);
    assertThat(subscription.isExecutionBlocked()).isTrue();

    subscription.removeSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.removeSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.removeSuspensionReason(PAYMENT_FAILED);
    assertThat(subscription.getSuspensionReasons()).isEmpty();
    assertThat(subscription.isExecutionBlocked()).isFalse();
  }

  @Test
  void doesNotExposeMutableSuspensionReasonState() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    Set<SubscriptionSuspensionReason> reasons = subscription.getSuspensionReasons();

    assertThatThrownBy(() -> reasons.add(PAYMENT_FAILED))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(subscription.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
  }

  @Test
  void isExecutionBlockedByLifecycleOrSuspensionReasons() {
    Subscription active = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    Subscription blockedActive = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    blockedActive.addSuspensionReason(PRODUCT_UNAVAILABLE);
    Subscription paused = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    paused.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    Subscription cancelled = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    cancelled.cancel();

    assertThat(active.isExecutionBlocked()).isFalse();
    assertThat(blockedActive.isExecutionBlocked()).isTrue();
    assertThat(paused.isExecutionBlocked()).isTrue();
    assertThat(cancelled.isExecutionBlocked()).isTrue();
  }

  @Test
  void subscriptionRuleExceptionsBelongToTheDomain() {
    assertThat(InvalidSubscriptionStateChangeException.class.getPackageName())
        .isEqualTo("com.bluetoya.beansontime.subscription.domain.exception");
    assertThat(InvalidSubscriptionPausePeriodException.class.getPackageName())
        .isEqualTo("com.bluetoya.beansontime.subscription.domain.exception");
    assertThat(InvalidSubscriptionResumeDateException.class.getPackageName())
        .isEqualTo("com.bluetoya.beansontime.subscription.domain.exception");
    assertThat(InvalidSubscriptionPeriodStateException.class.getPackageName())
        .isEqualTo("com.bluetoya.beansontime.subscription.domain.exception");
    assertThat(SubscriptionResumeRequiresPaymentException.class.getPackageName())
        .isEqualTo("com.bluetoya.beansontime.subscription.domain.exception");
  }

  private Subscription subscriptionStartedOn(LocalDate startedDate) {
    return new Subscription(
        new CustomerId(1),
        new ProductId(1),
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        startedDate);
  }

  private void assertActivePeriod(
      Subscription subscription, LocalDate startDate, LocalDate endDate) {
    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(startDate, endDate));
    assertThat(subscription.getRemainingPaidDays()).isNull();
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getScheduledResumeDate()).isNull();
    assertThat(subscription.getNextBillingDate()).isNotNull();
  }

  private void assertCancelledState(Subscription subscription) {
    assertThat(subscription.getLifecycleStatus()).isEqualTo(CANCELLED);
    assertThat(subscription.getCurrentPeriod()).isNull();
    assertThat(subscription.getRemainingPaidDays()).isNull();
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getScheduledResumeDate()).isNull();
    assertThat(subscription.getNextBillingDate()).isNull();
  }

  private long inclusiveDays(LocalDate startDate, LocalDate endDate) {
    return ChronoUnit.DAYS.between(startDate, endDate) + 1;
  }
}
