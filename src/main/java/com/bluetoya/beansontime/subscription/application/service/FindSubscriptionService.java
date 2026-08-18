package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.security.annotation.RequireSubscriptionOwner;
import com.bluetoya.beansontime.subscription.application.port.in.FindSubscriptionQuery;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.application.port.out.FindSubscriptionQueryPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindSubscriptionService implements FindSubscriptionQuery {
  private final FindSubscriptionQueryPort findSubscriptionQueryPort;

  @Override
  @RequireSubscriptionOwner
  public SubscriptionQueryResult find(SubscriptionId subscriptionId) {
    return findSubscriptionQueryPort.find(subscriptionId);
  }
}
