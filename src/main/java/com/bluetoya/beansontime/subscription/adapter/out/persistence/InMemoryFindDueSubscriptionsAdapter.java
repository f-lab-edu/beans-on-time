package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.subscription.application.port.out.FindDueSubscriptionsPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("in-memory")
@RequiredArgsConstructor
public class InMemoryFindDueSubscriptionsAdapter implements FindDueSubscriptionsPort {
  private final InMemorySubscriptionRepository subscriptions;

  public List<DueSubscription> findDue(LocalDate businessDate, SubscriptionId afterId, int limit) {
    return subscriptions.findDue(businessDate, afterId, limit).stream()
        .map(s -> new DueSubscription(s.getId(), s.getNextBillingDate()))
        .toList();
  }
}
