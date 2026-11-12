package com.bluetoya.beansontime.billing.application.port.out;

import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.Optional;

public interface FindRecurringBillingPort {
  Optional<Billing> findRecurring(SubscriptionId subscriptionId, LocalDate dueDate);
}
