package com.bluetoya.beansontime.billing.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.billing.application.port.in.BillingCheckoutDetail;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.adapter.out.persistence.InMemoryProductAdapter;
import com.bluetoya.beansontime.product.adapter.out.persistence.InMemoryProductRepository;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.InMemorySubscriptionRepository;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class InMemoryGetBillingCheckoutQueryAdapterTest {

  @Test
  void combinesBillingSubscriptionAndProductWhileDisplayingTheBillingPriceSnapshot() {
    InMemoryBillingRepository billingRepository = new InMemoryBillingRepository();
    InMemorySubscriptionRepository subscriptionRepository = new InMemorySubscriptionRepository();
    InMemoryProductRepository productRepository = new InMemoryProductRepository();
    Product product = new Product(new SellerId(1), "Ethiopia", new Money(35000));
    new InMemoryProductAdapter(productRepository).save(product);
    Subscription subscription = subscription(product.id());
    subscriptionRepository.save(subscription);
    Billing billing =
        new Billing(
            subscription.getCustomerId(),
            subscription.getId(),
            product.id(),
            new Money(30000),
            LocalDate.of(2026, 9, 2),
            LocalDateTime.of(2026, 9, 2, 10, 0));
    billingRepository.save(billing);
    InMemoryGetBillingCheckoutQueryAdapter adapter =
        new InMemoryGetBillingCheckoutQueryAdapter(
            billingRepository, subscriptionRepository, productRepository);

    BillingCheckoutDetail detail = adapter.get(billing.getId());

    assertThat(product.basePrice()).isEqualTo(new Money(35000));
    assertThat(detail.customerId()).isEqualTo(1);
    assertThat(detail.subscription().subscriptionId())
        .isEqualTo(subscription.getId().value().toString());
    assertThat(detail.subscription().lifecycleStatus()).isEqualTo("ACTIVE");
    assertThat(detail.product().productId()).isEqualTo(product.id().id());
    assertThat(detail.product().name()).isEqualTo("Ethiopia");
    assertThat(detail.billing().billingId()).isEqualTo(billing.getId().value());
    assertThat(detail.billing().amount()).isEqualTo(30000);
    assertThat(detail.billing().status()).isEqualTo("PENDING");
  }

  private Subscription subscription(ProductId productId) {
    return new Subscription(
        new CustomerId(1),
        productId,
        new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
        LocalDate.of(2026, 9, 2));
  }
}
