package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.security.annotation.RequireOwnership;
import com.bluetoya.beansontime.subscription.application.port.in.GetSubscriptionDetailQuery;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionDetail;
import com.bluetoya.beansontime.subscription.application.port.out.GetSubscriptionDetailQueryPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSubscriptionDetailService implements GetSubscriptionDetailQuery {
  private final GetSubscriptionDetailQueryPort getSubscriptionDetailQueryPort;

  @Override
  @RequireOwnership
  public SubscriptionDetail find(SubscriptionId subscriptionId) {
    return getSubscriptionDetailQueryPort.get(subscriptionId);
  }
}
