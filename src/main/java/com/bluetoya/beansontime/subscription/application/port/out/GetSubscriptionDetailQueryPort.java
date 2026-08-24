package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionDetail;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;

public interface GetSubscriptionDetailQueryPort {
  SubscriptionDetail get(SubscriptionId subscriptionId);
}
