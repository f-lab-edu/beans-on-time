package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.domain.SubscriptionId;

public interface SubscribeUseCase {
  SubscriptionId subscribe(SubscribeCommand command);
}
