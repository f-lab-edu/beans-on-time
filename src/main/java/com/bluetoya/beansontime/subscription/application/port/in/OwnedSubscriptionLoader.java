package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.application.policy.SubscriptionAccessPolicy;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnedSubscriptionLoader {
  private final LoadSubscriptionPort loadSubscriptionPort;
  private final SubscriptionAccessPolicy subscriptionAccessPolicy;

  public Subscription loadOwnedSubscription(CustomerId customerId, SubscriptionId subscriptionId) {
    Subscription subscription =
        loadSubscriptionPort.load(subscriptionId).orElseThrow(IllegalAccessError::new);
    subscriptionAccessPolicy.validateOwner(subscription, customerId);
    return subscription;
  }
}
