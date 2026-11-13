package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.List;

public interface FindDueAutoResumeSubscriptionsPort {
  List<SubscriptionId> findDue(LocalDate businessDate, SubscriptionId afterId, int limit);
}
