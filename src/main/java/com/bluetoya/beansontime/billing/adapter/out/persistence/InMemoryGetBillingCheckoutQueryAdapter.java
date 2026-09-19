package com.bluetoya.beansontime.billing.adapter.out.persistence;

import com.bluetoya.beansontime.billing.application.exception.BillingNotFoundException;
import com.bluetoya.beansontime.billing.application.port.in.BillingCheckoutDetail;
import com.bluetoya.beansontime.billing.application.port.out.GetBillingCheckoutQueryPort;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.product.adapter.out.persistence.InMemoryProductRepository;
import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.InMemorySubscriptionRepository;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryGetBillingCheckoutQueryAdapter implements GetBillingCheckoutQueryPort {
  private final InMemoryBillingRepository billingRepository;
  private final InMemorySubscriptionRepository subscriptionRepository;
  private final InMemoryProductRepository productRepository;

  @Override
  public BillingCheckoutDetail get(BillingId billingId) {
    Billing billing =
        billingRepository
            .findById(billingId)
            .orElseThrow(() -> new BillingNotFoundException("조회할 청구가 존재하지 않습니다."));
    Subscription subscription =
        subscriptionRepository
            .findById(billing.getSubscriptionId())
            .orElseThrow(() -> new SubscriptionNotFoundException("청구의 구독이 존재하지 않습니다."));
    Product product =
        productRepository
            .findById(billing.getProductId())
            .orElseThrow(() -> new ProductNotFoundException("청구의 상품이 존재하지 않습니다."));

    return new BillingCheckoutDetail(
        billing.getCustomerId().value(),
        new BillingCheckoutDetail.SubscriptionInfo(
            subscription.getId().value().toString(), subscription.getLifecycleStatus().name()),
        new BillingCheckoutDetail.ProductInfo(product.id().id(), product.name()),
        new BillingCheckoutDetail.BillingInfo(
            billing.getId().value(),
            billing.getAmount().price(),
            billing.getBillingDate(),
            billing.getStatus().name()));
  }
}
