package com.bluetoya.beansontime.product.application.service;

import static com.bluetoya.beansontime.product.domain.ProductStatus.AVAILABLE;
import static com.bluetoya.beansontime.product.domain.ProductStatus.DISCONTINUED;
import static com.bluetoya.beansontime.product.domain.ProductStatus.TEMPORARILY_UNAVAILABLE;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.ACTIVE;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.PAUSED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PAYMENT_FAILED;
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
import com.bluetoya.beansontime.product.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.InMemorySubscriptionAdapter;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.InMemorySubscriptionRepository;
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

class ChangeProductSupplyServiceTest {

  @Test
  void savesProductAndEveryUpdatedSubscription() {
    Product product = product();
    Subscription active = subscription(product.getId());
    Subscription paused = subscription(product.getId());
    paused.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    LoadSubscriptionsByProductPort loadSubscriptionsByProductPort =
        mock(LoadSubscriptionsByProductPort.class);
    SaveProductPort saveProductPort = mock(SaveProductPort.class);
    SaveSubscriptionPort saveSubscriptionPort = mock(SaveSubscriptionPort.class);
    when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));
    when(loadSubscriptionsByProductPort.loadNotCancelled(product.getId()))
        .thenReturn(List.of(active, paused));
    ChangeProductSupplyService service =
        new ChangeProductSupplyService(
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
  void stoppingSupplyBlocksActiveAndPausedSubscriptionsWithoutChangingLifecycle() {
    Product product = product();
    Subscription active = subscription(product.getId());
    Subscription paused = subscription(product.getId());
    paused.pause(LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 9, 10, 10, 0));
    ChangeProductSupplyService service = service(product, List.of(active, paused));

    service.stopSupply(product.getId());

    assertThat(product.getStatus()).isEqualTo(TEMPORARILY_UNAVAILABLE);
    assertThat(active.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(paused.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(active.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    assertThat(paused.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
  }

  @Test
  void resumingSupplyRemovesOnlyProductUnavailable() {
    Product product = product();
    product.stopSupply();
    Subscription subscription = subscription(product.getId());
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PAYMENT_FAILED);
    ChangeProductSupplyService service = service(product, List.of(subscription));

    service.resumeSupply(product.getId());

    assertThat(product.getStatus()).isEqualTo(AVAILABLE);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getSuspensionReasons()).containsExactly(PAYMENT_FAILED);
  }

  @Test
  void discontinuingBlocksActiveAndPausedSubscriptions() {
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
    InMemorySubscriptionAdapter subscriptionAdapter = new InMemorySubscriptionAdapter(repository);
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));
    ChangeProductSupplyService service =
        new ChangeProductSupplyService(
            new OwnedProductLoader(loadProductPort),
            mock(SaveProductPort.class),
            subscriptionAdapter,
            subscriptionAdapter);

    service.discontinue(product.getId());

    assertThat(product.getStatus()).isEqualTo(DISCONTINUED);
    assertThat(active.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(paused.getLifecycleStatus()).isEqualTo(PAUSED);
    assertThat(active.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    assertThat(paused.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    assertThat(cancelled.getSuspensionReasons()).isEmpty();
  }

  @Test
  void repeatedStopSupplyDoesNotDuplicateProductUnavailable() {
    Product product = product();
    Subscription subscription = subscription(product.getId());
    ChangeProductSupplyService service = service(product, List.of(subscription));

    service.stopSupply(product.getId());
    service.stopSupply(product.getId());

    assertThat(product.getStatus()).isEqualTo(TEMPORARILY_UNAVAILABLE);
    assertThat(subscription.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
  }

  @Test
  void repeatedResumeSupplyHasNoSubscriptionSideEffects() {
    Product product = product();
    product.stopSupply();
    Subscription subscription = subscription(product.getId());
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PAYMENT_FAILED);
    ChangeProductSupplyService service = service(product, List.of(subscription));

    service.resumeSupply(product.getId());
    service.resumeSupply(product.getId());

    assertThat(product.getStatus()).isEqualTo(AVAILABLE);
    assertThat(subscription.getSuspensionReasons()).containsExactly(PAYMENT_FAILED);
  }

  private ChangeProductSupplyService service(Product product, List<Subscription> subscriptions) {
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    LoadSubscriptionsByProductPort loadSubscriptionsByProductPort =
        mock(LoadSubscriptionsByProductPort.class);
    when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));
    when(loadSubscriptionsByProductPort.loadNotCancelled(product.getId()))
        .thenReturn(subscriptions);

    return new ChangeProductSupplyService(
        new OwnedProductLoader(loadProductPort),
        mock(SaveProductPort.class),
        loadSubscriptionsByProductPort,
        mock(SaveSubscriptionPort.class));
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
