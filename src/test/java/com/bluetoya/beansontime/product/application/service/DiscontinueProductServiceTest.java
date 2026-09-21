package com.bluetoya.beansontime.product.application.service;

import static com.bluetoya.beansontime.product.domain.SupplyStatus.DISCONTINUED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.ACTIVE;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.PAUSED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.application.port.in.OwnedProductLoader;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.application.port.out.SaveProductPort;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.InMemorySubscriptionAdapter;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.InMemorySubscriptionRepository;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiscontinueProductServiceTest {

  @Test
  void blocksActiveAndPausedSubscriptionsButExcludesCancelledSubscription() {
    Product product = product();
    Subscription active = subscription(product.getId());
    Subscription paused = subscription(product.getId());
    paused.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    paused.addSuspensionReason(PRODUCT_UNAVAILABLE);
    Subscription cancelled = subscription(product.getId());
    cancelled.cancel();
    InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
    repository.save(active);
    repository.save(paused);
    repository.save(cancelled);
    InMemorySubscriptionAdapter subscriptionAdapter =
        spy(new InMemorySubscriptionAdapter(repository));
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    SaveProductPort saveProductPort = mock(SaveProductPort.class);
    when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));
    DiscontinueProductService service =
        new DiscontinueProductService(
            new OwnedProductLoader(loadProductPort),
            saveProductPort,
            subscriptionAdapter,
            subscriptionAdapter);

    service.discontinue(product.getId());

    assertThat(product.getSupplyStatus()).isEqualTo(DISCONTINUED);
    assertThat(active.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(paused.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(active.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    assertThat(paused.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    assertThat(cancelled.getSuspensionReasons()).isEmpty();
    verify(saveProductPort).save(product);
    verify(subscriptionAdapter).save(active);
    verify(subscriptionAdapter).save(paused);
    verify(subscriptionAdapter, never()).save(cancelled);
  }

  private Product product() {
    return new Product(new SellerId(1), "Ethiopia", new Money(18000));
  }

  private Subscription subscription(ProductId productId) {
    return new Subscription(
        new CustomerId(1),
        productId,
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        LocalDate.of(2026, 9, 1));
  }
}
