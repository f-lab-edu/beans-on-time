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
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionResumeDateException;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionStateChangeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

  private static final LocalDateTime PAUSED_AT = LocalDateTime.of(2026, 9, 10, 14, 30);

  @Test
  void initializesRollingPeriodFromJanuaryThirtyFirst() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 1, 31));

    assertThat(subscription.getStartedDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(31));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    assertThat(subscription.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 27)));
    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getSuspensionReasons()).isEmpty();
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getResumeDate()).isNull();
  }

  @Test
  void adjustsJanuaryThirtyFirstToLeapYearFebruary() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2028, 1, 31));

    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2028, 2, 29));
    assertThat(subscription.getCurrentPeriod().endDate())
        .isEqualTo(subscription.getNextBillingDate().minusDays(1));
  }

  @Test
  void adjustsAnchorDayThirtyToTheLastDayOfFebruary() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 1, 30));

    assertThat(subscription.getBillingAnchorDay()).isEqualTo(new BillingAnchorDay(30));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void pauseImmediatelyBlocksExecutionWithoutChangingPeriodOrBillingDate() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    SubscriptionPeriod currentPeriod = subscription.getCurrentPeriod();
    LocalDate nextBillingDate = subscription.getNextBillingDate();

    subscription.pause(LocalDate.of(2026, 9, 20), PAUSED_AT);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(subscription.isExecutionBlocked()).isTrue();
    assertThat(subscription.getPausedAt()).isEqualTo(PAUSED_AT);
    assertThat(subscription.getResumeDate()).isEqualTo(LocalDate.of(2026, 9, 21));
    assertThat(subscription.getCurrentPeriod()).isEqualTo(currentPeriod);
    assertThat(subscription.getNextBillingDate()).isEqualTo(nextBillingDate);
  }

  @Test
  void pauseBeyondNextBillingDateStillPreservesBillingDate() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));

    subscription.pause(LocalDate.of(2026, 10, 20), PAUSED_AT);

    assertThat(subscription.getResumeDate()).isEqualTo(LocalDate.of(2026, 10, 21));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 10, 1));
  }

  @Test
  void rejectsPauseUntilDateBeforePausedAtDateWithoutChangingState() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    SubscriptionPeriod currentPeriod = subscription.getCurrentPeriod();
    LocalDate nextBillingDate = subscription.getNextBillingDate();

    assertThatThrownBy(() -> subscription.pause(LocalDate.of(2026, 9, 9), PAUSED_AT))
        .isInstanceOf(InvalidSubscriptionPausePeriodException.class);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getCurrentPeriod()).isEqualTo(currentPeriod);
    assertThat(subscription.getNextBillingDate()).isEqualTo(nextBillingDate);
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getResumeDate()).isNull();
  }

  @Test
  void allowsPauseUntilDateEqualToPausedAtDate() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));

    subscription.pause(LocalDate.of(2026, 9, 10), PAUSED_AT);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(subscription.getResumeDate()).isEqualTo(LocalDate.of(2026, 9, 11));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 10, 1));
  }

  @Test
  void cannotPauseAgainWhilePaused() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    subscription.pause(LocalDate.of(2026, 9, 20), PAUSED_AT);

    assertThatThrownBy(() -> subscription.pause(LocalDate.of(2026, 10, 15), PAUSED_AT.plusDays(1)))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);
  }

  @Test
  void cannotPauseOrResumeAfterCancellation() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    subscription.cancel();

    assertThatThrownBy(() -> subscription.pause(LocalDate.of(2026, 9, 20), PAUSED_AT))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);
    assertThatThrownBy(() -> subscription.resume(LocalDate.of(2026, 9, 20)))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);
  }

  @Test
  void cancelIsIdempotentFromActiveState() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));

    subscription.cancel();
    subscription.cancel();

    assertThat(subscription.getLifecycleStatus()).isEqualTo(CANCELLED);
  }

  @Test
  void cancelClearsPausedContextAndRemainsIdempotent() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    subscription.pause(LocalDate.of(2026, 9, 20), PAUSED_AT);

    subscription.cancel();
    subscription.cancel();

    assertThat(subscription.getLifecycleStatus()).isEqualTo(CANCELLED);
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getResumeDate()).isNull();
  }

  @Test
  void suspensionReasonDoesNotPreventCustomerPause() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);

    subscription.pause(LocalDate.of(2026, 9, 20), PAUSED_AT);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(subscription.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
  }

  @Test
  void manualResumeBeforeScheduledResumeDatePreservesCurrentPeriodAndSuspensionReasons() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    SubscriptionPeriod currentPeriod = subscription.getCurrentPeriod();
    LocalDate nextBillingDate = subscription.getNextBillingDate();
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.pause(LocalDate.of(2026, 9, 25), PAUSED_AT);
    assertThat(subscription.getResumeDate()).isEqualTo(LocalDate.of(2026, 9, 26));

    subscription.resume(LocalDate.of(2026, 9, 20));

    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getCurrentPeriod()).isEqualTo(currentPeriod);
    assertThat(subscription.getNextBillingDate()).isEqualTo(nextBillingDate);
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getResumeDate()).isNull();
    assertThat(subscription.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    assertThat(subscription.isExecutionBlocked()).isTrue();
  }

  @Test
  void manualResumeAfterScheduledResumeDateIsAllowedWhileStillPaused() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    subscription.pause(LocalDate.of(2026, 9, 29), PAUSED_AT);
    assertThat(subscription.getResumeDate()).isEqualTo(LocalDate.of(2026, 9, 30));

    subscription.resume(LocalDate.of(2026, 10, 2));

    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 31)));
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getResumeDate()).isNull();
  }

  @Test
  void rejectsResumeDateBeforePauseRequestDateWithoutChangingState() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    SubscriptionPeriod currentPeriod = subscription.getCurrentPeriod();
    LocalDate nextBillingDate = subscription.getNextBillingDate();
    subscription.pause(LocalDate.of(2026, 9, 20), PAUSED_AT);

    assertThatThrownBy(() -> subscription.resume(LocalDate.of(2026, 9, 9)))
        .isInstanceOf(InvalidSubscriptionResumeDateException.class);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(subscription.getCurrentPeriod()).isEqualTo(currentPeriod);
    assertThat(subscription.getNextBillingDate()).isEqualTo(nextBillingDate);
    assertThat(subscription.getPausedAt()).isEqualTo(PAUSED_AT);
    assertThat(subscription.getResumeDate()).isEqualTo(LocalDate.of(2026, 9, 21));
  }

  @Test
  void allowsResumeOnPauseRequestDateWithoutReplacingCurrentPeriod() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    SubscriptionPeriod currentPeriod = subscription.getCurrentPeriod();
    subscription.pause(LocalDate.of(2026, 9, 20), PAUSED_AT);

    subscription.resume(LocalDate.of(2026, 9, 10));

    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getCurrentPeriod()).isEqualTo(currentPeriod);
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getResumeDate()).isNull();
  }

  @Test
  void resumeAfterCurrentPeriodConfirmsANewBillingPeriodFromAnchorDay() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 8, 31));
    subscription.pause(LocalDate.of(2026, 10, 20), PAUSED_AT);

    subscription.resume(LocalDate.of(2026, 10, 10));

    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 30));
    assertThat(subscription.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 11, 29)));
    assertThat(subscription.getCurrentPeriod().endDate())
        .isEqualTo(subscription.getNextBillingDate().minusDays(1));
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getResumeDate()).isNull();
  }

  @Test
  void cannotResumeAnActiveSubscription() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));

    assertThatThrownBy(() -> subscription.resume(LocalDate.of(2026, 9, 20)))
        .isInstanceOf(InvalidSubscriptionStateChangeException.class);
  }

  @Test
  void managesMultipleSuspensionReasonsIdempotentlyAndIndependentlyFromLifecycle() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));

    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PAYMENT_FAILED);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getSuspensionReasons())
        .containsExactlyInAnyOrder(PRODUCT_UNAVAILABLE, PAYMENT_FAILED);

    subscription.removeSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.removeSuspensionReason(PRODUCT_UNAVAILABLE);

    assertThat(subscription.getSuspensionReasons()).containsExactly(PAYMENT_FAILED);

    subscription.removeSuspensionReason(PAYMENT_FAILED);

    assertThat(subscription.getSuspensionReasons()).isEmpty();
    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
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
    Subscription productBlockedActive = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    productBlockedActive.addSuspensionReason(PRODUCT_UNAVAILABLE);
    Subscription paymentBlockedActive = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    paymentBlockedActive.addSuspensionReason(PAYMENT_FAILED);
    Subscription paused = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    paused.pause(LocalDate.of(2026, 9, 20), PAUSED_AT);
    Subscription blockedPaused = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    blockedPaused.addSuspensionReason(PRODUCT_UNAVAILABLE);
    blockedPaused.pause(LocalDate.of(2026, 9, 20), PAUSED_AT);
    Subscription cancelled = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    cancelled.cancel();

    assertThat(active.isExecutionBlocked()).isFalse();
    assertThat(productBlockedActive.isExecutionBlocked()).isTrue();
    assertThat(paymentBlockedActive.isExecutionBlocked()).isTrue();
    assertThat(paused.isExecutionBlocked()).isTrue();
    assertThat(blockedPaused.isExecutionBlocked()).isTrue();
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
  }

  private Subscription subscriptionStartedOn(LocalDate startedDate) {
    return new Subscription(
        new CustomerId(1), new ProductId(1), new Cycle(CycleUnit.ONE_MONTH, 1), startedDate);
  }
}
