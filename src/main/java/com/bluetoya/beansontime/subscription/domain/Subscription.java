package com.bluetoya.beansontime.subscription.domain;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Subscription {
  private final SubscriptionId subscriptionId;
  private final CustomerId customerId;
  private final ProductId productId;
  private Cycle cycle;
  private SubscriptionStatus subscriptionStatus;

  public Subscription(CustomerId customerId, ProductId productId, Cycle cycle) {
    this.subscriptionId = SubscriptionId.generate();
    this.customerId = customerId;
    this.productId = productId;
    this.cycle = cycle;
    this.subscriptionStatus = SubscriptionStatus.ACTIVE;
  }

  public boolean isOwnedBy(CustomerId customerId) {
    return this.customerId.equals(customerId);
  }

  public void pause() {
    if (this.subscriptionStatus != SubscriptionStatus.ACTIVE) {
      throw new IllegalArgumentException("일시정지 불가능한 구독입니다.");
    }
    this.subscriptionStatus = SubscriptionStatus.HOLD;
  }

  public void resume() {
    if (this.subscriptionStatus != SubscriptionStatus.HOLD) {
      throw new IllegalArgumentException("구독 재개가 불가능합니다.");
    }
    this.subscriptionStatus = SubscriptionStatus.ACTIVE;
  }
}
