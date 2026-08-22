package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.security.annotation.RequireOwnership;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnedSubscriptionLoader {
  private final LoadSubscriptionPort loadSubscriptionPort;

  @RequireOwnership
  public Subscription load(SubscriptionId subscriptionId) {
    return loadSubscriptionPort.load(subscriptionId).orElseThrow();
  }
}
