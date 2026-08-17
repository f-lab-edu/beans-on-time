package com.bluetoya.beansontime.subscription.application.policy;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionAccessPolicy {
  public void validateOwner(Subscription subscription, CustomerId requestedCustomerId) {
    if (!subscription.isOwnedBy(requestedCustomerId)) {
      throw new IllegalArgumentException("고객이 신청한 구독이 아닙니다.");
    }
  }
}
