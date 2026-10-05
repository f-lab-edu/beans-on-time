package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.subscription.domain.Subscription;

public interface SaveSubscriptionPort {
  void saveNew(Subscription subscription);

  void save(Subscription subscription);
}
