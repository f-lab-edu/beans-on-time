package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.port.out.ProductExecutionPort;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SubscriptionExecutionPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionExecutionAdapter implements SubscriptionExecutionPort {
  private final LoadSubscriptionPort subscriptions;
  private final ProductExecutionPort productExecutionPort;

  public <T> T execute(SubscriptionId id, Supplier<T> action) {
    var subscription = subscriptions.load(id);
    if (subscription.isEmpty()) return action.get();
    return productExecutionPort.execute(subscription.get().getProductId(), action);
  }
}
