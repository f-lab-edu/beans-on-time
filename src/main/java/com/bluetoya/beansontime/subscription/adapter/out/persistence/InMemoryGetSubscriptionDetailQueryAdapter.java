package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.product.adapter.out.persistence.InMemoryProductRepository;
import com.bluetoya.beansontime.product.application.port.in.ProductAvailability;
import com.bluetoya.beansontime.product.application.port.in.ProductInfo;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionDetail;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionInfo;
import com.bluetoya.beansontime.subscription.application.port.out.GetSubscriptionDetailQueryPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import com.bluetoya.beansontime.subscription.domain.SubscriptionPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryGetSubscriptionDetailQueryAdapter implements GetSubscriptionDetailQueryPort {

  private final InMemorySubscriptionRepository subscriptionRepository;
  private final InMemoryProductRepository productRepository;

  public SubscriptionDetail get(SubscriptionId subscriptionId) {
    Subscription subscription =
        subscriptionRepository
            .findById(subscriptionId)
            .orElseThrow(() -> new SubscriptionNotFoundException("조회할 구독이 존재하지 않습니다."));

    ProductInfo productInfo =
        productRepository
            .findById(subscription.getProductId())
            .map(this::toProductInfo)
            .orElseGet(() -> ProductInfo.toUnavailableProductInfo(subscription.getProductId()));

    return new SubscriptionDetail(toSubscriptionInfo(subscription), productInfo);
  }

  private SubscriptionInfo toSubscriptionInfo(Subscription subscription) {
    SubscriptionPeriod currentPeriod = subscription.getCurrentPeriod();

    return new SubscriptionInfo(
        subscription.getId().value().toString(),
        subscription.getCustomerId().value(),
        subscription.getDeliveryCycle().unit().name(),
        subscription.getDeliveryCycle().interval(),
        subscription.getLifecycleStatus().name(),
        subscription.getSuspensionReasons(),
        subscription.getStartedDate(),
        currentPeriod == null ? null : currentPeriod.startDate(),
        currentPeriod == null ? null : currentPeriod.endDate(),
        subscription.getRemainingPaidDays(),
        subscription.getBillingAnchorDay().value(),
        subscription.getNextBillingDate(),
        subscription.getPausedAt(),
        subscription.getScheduledResumeDate(),
        subscription.isExecutionBlocked());
  }

  private ProductInfo toProductInfo(Product product) {
    return new ProductInfo(
        ProductAvailability.AVAILABLE,
        product.id().id(),
        product.name(),
        product.basePrice().price());
  }
}
