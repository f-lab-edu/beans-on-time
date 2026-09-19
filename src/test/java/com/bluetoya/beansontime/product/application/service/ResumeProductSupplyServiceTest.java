package com.bluetoya.beansontime.product.application.service;

import static com.bluetoya.beansontime.product.domain.SupplyStatus.AVAILABLE;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionStatus.ACTIVE;
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
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionsByProductPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResumeProductSupplyServiceTest {

  @Test
  void removesOnlyProductUnavailable() {
    Product product = temporarilyUnavailableProduct();
    Subscription subscription = subscription(product.getId());
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PAYMENT_FAILED);
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    LoadSubscriptionsByProductPort loadSubscriptionsByProductPort =
        mock(LoadSubscriptionsByProductPort.class);
    SaveProductPort saveProductPort = mock(SaveProductPort.class);
    SaveSubscriptionPort saveSubscriptionPort = mock(SaveSubscriptionPort.class);
    when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));
    when(loadSubscriptionsByProductPort.loadNotCancelled(product.getId()))
        .thenReturn(List.of(subscription));
    ResumeProductSupplyService service =
        new ResumeProductSupplyService(
            new OwnedProductLoader(loadProductPort),
            saveProductPort,
            loadSubscriptionsByProductPort,
            saveSubscriptionPort);

    service.resumeSupply(product.getId());

    assertThat(product.getSupplyStatus()).isEqualTo(AVAILABLE);
    assertThat(subscription.getLifecycleStatus()).isEqualTo(ACTIVE);
    assertThat(subscription.getSuspensionReasons()).containsExactly(PAYMENT_FAILED);
    verify(saveProductPort).save(product);
    verify(saveSubscriptionPort).save(subscription);
  }

  @Test
  void repeatedRequestHasNoSubscriptionSideEffects() {
    Product product = temporarilyUnavailableProduct();
    Subscription subscription = subscription(product.getId());
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PAYMENT_FAILED);
    ResumeProductSupplyService service = service(product, List.of(subscription));

    service.resumeSupply(product.getId());
    service.resumeSupply(product.getId());

    assertThat(product.getSupplyStatus()).isEqualTo(AVAILABLE);
    assertThat(subscription.getSuspensionReasons()).containsExactly(PAYMENT_FAILED);
  }

  private ResumeProductSupplyService service(Product product, List<Subscription> subscriptions) {
    LoadProductPort loadProductPort = mock(LoadProductPort.class);
    LoadSubscriptionsByProductPort loadSubscriptionsByProductPort =
        mock(LoadSubscriptionsByProductPort.class);
    when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));
    when(loadSubscriptionsByProductPort.loadNotCancelled(product.getId()))
        .thenReturn(subscriptions);
    return new ResumeProductSupplyService(
        new OwnedProductLoader(loadProductPort),
        mock(SaveProductPort.class),
        loadSubscriptionsByProductPort,
        mock(SaveSubscriptionPort.class));
  }

  private Product temporarilyUnavailableProduct() {
    Product product = new Product(new SellerId(1), "Ethiopia", new Money(18000));
    product.stopSupply();
    return product;
  }

  private Subscription subscription(ProductId productId) {
    return new Subscription(
        new CustomerId(1),
        productId,
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        LocalDate.of(2026, 9, 1));
  }
}
