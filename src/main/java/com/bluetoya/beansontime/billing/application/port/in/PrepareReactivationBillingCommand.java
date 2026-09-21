package com.bluetoya.beansontime.billing.application.port.in;

import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.Objects;

public record PrepareReactivationBillingCommand(SubscriptionId subscriptionId) {
  public PrepareReactivationBillingCommand {
    Objects.requireNonNull(subscriptionId, "구독 ID는 필수입니다.");
  }
}
