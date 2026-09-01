package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.application.port.out.ExistsSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemorySubscriptionAdapter
    implements SaveSubscriptionPort, ExistsSubscriptionPort, LoadSubscriptionPort {

  private final InMemorySubscriptionRepository subscriptionRepository;

  @Override
  public void save(Subscription subscription) {
    subscriptionRepository.save(subscription);
  }

  @Override
  public boolean isExists(CustomerId customerId, ProductId productId) {
    return subscriptionRepository.isExists(customerId, productId);
  }

  @Override
  public Optional<Subscription> load(SubscriptionId subscriptionId) {
    return subscriptionRepository.findById(subscriptionId);
  }
}
