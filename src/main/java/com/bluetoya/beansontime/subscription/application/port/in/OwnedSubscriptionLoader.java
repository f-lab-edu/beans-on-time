package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.security.application.CurrentCustomerProvider;
import com.bluetoya.beansontime.subscription.application.policy.SubscriptionAccessPolicy;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnedSubscriptionLoader {
  private final LoadSubscriptionPort loadSubscriptionPort;
  private final SubscriptionAccessPolicy subscriptionAccessPolicy;
  private final CurrentCustomerProvider currentCustomerProvider;

  public Subscription loadOwnedSubscription(SubscriptionId subscriptionId) {
    Subscription subscription =
        loadSubscriptionPort.load(subscriptionId).orElseThrow(IllegalAccessError::new);

    CustomerId customerId = currentCustomerProvider.getCurrentCustomerId();

    subscriptionAccessPolicy.validateOwner(subscription, customerId);
    return subscription;
  }
}
