package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.application.port.out.ExistsSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.FindSubscriptionQueryPort;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.*;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemorySubscriptionAdapter
    implements SaveSubscriptionPort,
        ExistsSubscriptionPort,
        FindSubscriptionQueryPort,
        LoadSubscriptionPort {
  private final Map<SubscriptionId, Subscription> subscriptions = new ConcurrentHashMap<>();

  @Override
  public void save(Subscription subscription) {
    subscriptions.put(subscription.getSubscriptionId(), subscription);
  }

  @Override
  public SubscriptionQueryResult find(SubscriptionId subscriptionId) {
    Subscription subscription = subscriptions.get(subscriptionId);

    if (Objects.isNull(subscription)) {
      throw new SubscriptionNotFoundException("구독 내역을 찾을 수 없습니다.");
    }

    return new SubscriptionQueryResult(
        subscription.getSubscriptionId().value(),
        subscription.getCustomerId().id(),
        subscription.getProductId().id(),
        subscription.getCycle().getUnit().name(),
        subscription.getCycle().getInterval(),
        subscription.getSubscriptionStatus().name());
  }

  @Override
  public boolean isExists(CustomerId customerId, ProductId productId) {
    return subscriptions.values().stream()
        .anyMatch(
            subscription ->
                subscription.getCustomerId().equals(customerId)
                    && subscription.getProductId().equals(productId)
                    && subscription.getSubscriptionStatus() != SubscriptionStatus.CANCEL);
  }

  @Override
  public Optional<Subscription> load(SubscriptionId subscriptionId) {
    return Optional.ofNullable(subscriptions.get(subscriptionId));
  }
}
