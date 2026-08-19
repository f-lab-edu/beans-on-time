package com.bluetoya.beansontime.subscription.fixture;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.*;

public class SubscriptionFixture {
  private SubscriptionFixture() {}

  public static Subscription createSubscription() {
    return new Subscription(
        new CustomerId(1L), new ProductId(1L), new Cycle(CycleUnit.ONE_MONTH, 1));
  }

  public static Subscription createPausedSubscription() {
    Subscription subscription = createSubscription();
    subscription.pause();
    return subscription;
  }
}
