package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PAYMENT_FAILED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.adapter.out.persistence.InMemoryProductAdapter;
import com.bluetoya.beansontime.product.adapter.out.persistence.InMemoryProductRepository;
import com.bluetoya.beansontime.product.application.port.in.ProductAvailability;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.product.domain.SellerId;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionDetail;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionInfo;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycle;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryGetSubscriptionDetailQueryAdapterTest {

  private InMemorySubscriptionRepository subscriptionRepository;
  private InMemoryProductRepository productRepository;
  private InMemoryGetSubscriptionDetailQueryAdapter adapter;

  @BeforeEach
  void setUp() {
    subscriptionRepository = new InMemorySubscriptionRepository();
    productRepository = new InMemoryProductRepository();
    adapter =
        new InMemoryGetSubscriptionDetailQueryAdapter(subscriptionRepository, productRepository);
  }

  @Test
  void projectsSubscriptionTimeAndBlockingStateWithAvailableProduct() {
    Product product = new Product(new SellerId(2), "Ethiopia", new Money(18000));
    new InMemoryProductAdapter(productRepository).save(product);
    Subscription subscription =
        new Subscription(
            new CustomerId(1),
            product.getId(),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            LocalDate.of(2026, 8, 31));
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.addSuspensionReason(PAYMENT_FAILED);
    subscription.pause(LocalDate.of(2026, 10, 15), LocalDateTime.of(2026, 9, 10, 14, 30));
    subscriptionRepository.save(subscription);

    SubscriptionDetail detail = adapter.get(subscription.getId());

    SubscriptionInfo info = detail.subscriptionInfo();
    assertThat(info.subscriptionId()).isEqualTo(subscription.getId().value().toString());
    assertThat(info.customerId()).isEqualTo(1);
    assertThat(info.deliveryCycleUnit()).isEqualTo("ONE_MONTH");
    assertThat(info.deliveryCycleInterval()).isEqualTo(1);
    assertThat(info.lifecycleStatus()).isEqualTo(SubscriptionStatus.PAUSED.name());
    assertThat(info.suspensionReasons())
        .containsExactlyInAnyOrder(PRODUCT_UNAVAILABLE, PAYMENT_FAILED);
    assertThat(info.startedDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(info.currentPeriodStartDate()).isNull();
    assertThat(info.currentPeriodEndDate()).isNull();
    assertThat(info.remainingPaidDays()).isEqualTo(19);
    assertThat(info.billingAnchorDay()).isEqualTo(31);
    assertThat(info.nextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 4));
    assertThat(info.pausedAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 14, 30));
    assertThat(info.scheduledResumeDate()).isEqualTo(LocalDate.of(2026, 10, 16));
    assertThat(info.executionBlocked()).isTrue();
    assertThat(detail.productInfo().availability()).isEqualTo(ProductAvailability.AVAILABLE);
    assertThat(detail.productInfo().productId()).isEqualTo(product.getId().id());
    assertThat(detail.productInfo().name()).isEqualTo("Ethiopia");
    assertThat(detail.productInfo().basePrice()).isEqualTo(18000);
  }

  @Test
  void keepsReferencedProductIdAndUsesUnavailableFallbackWhenProductIsMissing() {
    Subscription subscription =
        new Subscription(
            new CustomerId(1),
            new ProductId(999),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            LocalDate.of(2026, 8, 31));
    subscriptionRepository.save(subscription);

    SubscriptionDetail detail = adapter.get(subscription.getId());

    assertThat(detail.productInfo().availability()).isEqualTo(ProductAvailability.UNAVAILABLE);
    assertThat(detail.productInfo().productId()).isEqualTo(999);
    assertThat(detail.productInfo().name()).isNull();
    assertThat(detail.productInfo().basePrice()).isNull();
  }
}
