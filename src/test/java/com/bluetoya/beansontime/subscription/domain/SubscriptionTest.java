package com.bluetoya.beansontime.subscription.domain;

import static com.bluetoya.beansontime.subscription.fixture.SubscriptionFixture.createPausedSubscription;
import static com.bluetoya.beansontime.subscription.fixture.SubscriptionFixture.createSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.subscription.application.exception.InvalidSubscriptionStateChangeException;
import org.junit.jupiter.api.Test;

public class SubscriptionTest {
  @Test
  void 활성_중인_구독을_일시정지하면_PAUSED_상태가_된다() {
    // given
    Subscription subscription = createSubscription();

    // when
    subscription.pause();

    // then
    assertThat(subscription.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAUSED);
  }

  @Test
  void 이미_일시정지된_구독은_다시_일시정지할_수_없다() {
    // given
    Subscription subscription = createPausedSubscription();

    // when & then
    assertThatThrownBy(subscription::pause).isInstanceOf(InvalidSubscriptionStateChangeException.class);
  }

  @Test
  void 일시정지된_구독을_재개하면_ACTIVE_상태가_된다() {
    // given
    Subscription subscription = createPausedSubscription();

    // when
    subscription.resume();

    // then
    assertThat(subscription.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void 활성화된_구독은_재개할_수_없다() {
    // given
    Subscription subscription = createSubscription();

    // when & then
    assertThatThrownBy(subscription::resume).isInstanceOf(InvalidSubscriptionStateChangeException.class);
  }

  @Test
  void 구독에_저장된_고객이_비교하는_고객과_동일하면_true를_반환한다() {
    // given
    Subscription subscription = createSubscription();
    CustomerId requestedCustomerId = new CustomerId(1L);

    // when
    boolean owned = subscription.isOwnedBy(requestedCustomerId);

    // then
    assertThat(owned).isTrue();
  }

  @Test
  void 구독에_저장된_고객과_비교하는_고객이_다르면_false를_반환한다() {
    // given
    Subscription subscription = createSubscription();
    CustomerId requestedCustomerId = new CustomerId(2L);

    // when
    boolean owned = subscription.isOwnedBy(requestedCustomerId);

    // then
    assertThat(owned).isFalse();
  }
}
