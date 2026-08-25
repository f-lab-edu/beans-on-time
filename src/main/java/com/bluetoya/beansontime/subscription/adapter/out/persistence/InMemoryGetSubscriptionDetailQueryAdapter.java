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
    return new SubscriptionInfo(
        subscription.getId().value().toString(),
        subscription.getCustomerId().value(),
        subscription.getCycle().getUnit().name(),
        subscription.getCycle().getInterval(),
        subscription.getLifecycleStatus().name(),
        subscription.getSuspensionReasons(),
        subscription.getStartedDate(),
        subscription.getCurrentPeriod().startDate(),
        subscription.getCurrentPeriod().endDate(),
        subscription.getBillingAnchorDay().value(),
        subscription.getNextBillingDate(),
        subscription.getPausedAt(),
        subscription.getResumeDate(),
        subscription.isExecutionBlocked());
  }

  private ProductInfo toProductInfo(Product product) {
    return new ProductInfo(
        ProductAvailability.AVAILABLE,
        product.getId().id(),
        product.getName(),
        product.getBasePrice().price());
  }
}
