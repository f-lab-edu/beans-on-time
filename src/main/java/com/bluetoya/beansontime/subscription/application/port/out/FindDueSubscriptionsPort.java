package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.List;

public interface FindDueSubscriptionsPort {
  record DueSubscription(SubscriptionId subscriptionId, LocalDate dueDate) {}

  List<DueSubscription> findDue(LocalDate businessDate, SubscriptionId afterId, int limit);
}
