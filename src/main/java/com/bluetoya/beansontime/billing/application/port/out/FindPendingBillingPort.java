package com.bluetoya.beansontime.billing.application.port.out;

import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.Optional;

public interface FindPendingBillingPort {
  Optional<Billing> findPending(SubscriptionId subscriptionId);
}
