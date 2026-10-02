package com.bluetoya.beansontime.product.application.service;

import static com.bluetoya.beansontime.product.domain.SupplyStatus.TEMPORARILY_UNAVAILABLE;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.ACTIVE;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.PAUSED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionsByProductPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StopProductSupplyServiceTest {

  @Test
  void savesProductAndEveryUpdatedSubscription() {
    Product product = product();
    Subscription active = subscription(product.getId());
    Subscription paused = pausedSubscription(product.getId());
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    LoadSubscriptionsByProductPort loadSubscriptionsByProductPort =
        mock(LoadSubscriptionsByProductPort.class);
    SaveProductPort saveProductPort = mock(SaveProductPort.class);
    SaveSubscriptionPort saveSubscriptionPort = mock(SaveSubscriptionPort.class);
    when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));
    when(loadSubscriptionsByProductPort.loadNotCancelled(product.getId()))
        .thenReturn(List.of(active, paused));
    StopProductSupplyService service =
        new StopProductSupplyService(
            new OwnedProductLoader(loadProductPort),
            saveProductPort,
            loadSubscriptionsByProductPort,
            saveSubscriptionPort);

    service.stopSupply(product.getId());

    verify(saveProductPort).save(product);
    verify(saveSubscriptionPort).save(active);
    verify(saveSubscriptionPort).save(paused);
  }

  @Test
  void blocksActiveAndPausedSubscriptionsWithoutChangingLifecycle() {
    Product product = product();
    Subscription active = subscription(product.getId());
    Subscription paused = pausedSubscription(product.getId());
    StopProductSupplyService service = service(product, List.of(active, paused));

    service.stopSupply(product.getId());

    assertThat(product.getSupplyStatus()).isEqualTo(TEMPORARILY_UNAVAILABLE);
    assertThat(active.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(paused.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(active.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    assertThat(paused.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
  }

  @Test
  void repeatedRequestDoesNotDuplicateProductUnavailable() {
    Product product = product();
    Subscription subscription = subscription(product.getId());
    StopProductSupplyService service = service(product, List.of(subscription));

    service.stopSupply(product.getId());
    service.stopSupply(product.getId());

    assertThat(product.getSupplyStatus()).isEqualTo(TEMPORARILY_UNAVAILABLE);
    assertThat(subscription.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
  }

  private StopProductSupplyService service(Product product, List<Subscription> subscriptions) {
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    LoadSubscriptionsByProductPort loadSubscriptionsByProductPort =
        mock(LoadSubscriptionsByProductPort.class);
    when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));
    when(loadSubscriptionsByProductPort.loadNotCancelled(product.getId()))
        .thenReturn(subscriptions);
    return new StopProductSupplyService(
        new OwnedProductLoader(loadProductPort),
        mock(SaveProductPort.class),
        loadSubscriptionsByProductPort,
        mock(SaveSubscriptionPort.class));
  }

  private Product product() {
    return new Product(new SellerId(1), "Ethiopia", new Money(18000));
  }

  private Subscription pausedSubscription(ProductId productId) {
    Subscription subscription = subscription(productId);
    subscription.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    return subscription;
  }

  private Subscription subscription(ProductId productId) {
    return new Subscription(
        new CustomerId(1),
        productId,
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        LocalDate.of(2026, 9, 1));
  }
}
