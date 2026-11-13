package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.subscription.application.port.out.FindDueAutoResumeSubscriptionsPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("in-memory")
@RequiredArgsConstructor
public class InMemoryFindDueAutoResumeSubscriptionsAdapter
    implements FindDueAutoResumeSubscriptionsPort {
  private final InMemorySubscriptionRepository subscriptions;

  @Override
  public List<SubscriptionId> findDue(LocalDate businessDate, SubscriptionId afterId, int limit) {
    return subscriptions.findDueAutoResume(businessDate, afterId, limit);
  }
}
