package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class InMemorySubscriptionAdapterTest {

  @Test
  void loadsActiveAndPausedSubscriptionsForProductButExcludesCancelledAndOtherProducts() {
    InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
    InMemorySubscriptionAdapter adapter = new InMemorySubscriptionAdapter(repository);
    ProductId productId = new ProductId(10);
    Subscription active = subscription(productId);
    Subscription paused = subscription(productId);
    paused.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    Subscription cancelled = subscription(productId);
    cancelled.cancel();
    Subscription anotherProduct = subscription(new ProductId(20));
    repository.save(active);
    repository.save(paused);
    repository.save(cancelled);
    repository.save(anotherProduct);

    assertThat(adapter.loadNotCancelled(productId)).containsExactlyInAnyOrder(active, paused);
  }

  private Subscription subscription(ProductId productId) {
    return new Subscription(
        new CustomerId(1),
        productId,
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        LocalDate.of(2026, 9, 1));
  }
}
