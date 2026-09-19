package com.bluetoya.beansontime.product.application.service;

import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;

import com.bluetoya.beansontime.product.application.port.in.OwnedProductLoader;
import com.bluetoya.beansontime.product.application.port.in.StopProductSupplyUseCase;
import com.bluetoya.beansontime.product.application.port.out.SaveProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionsByProductPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StopProductSupplyService implements StopProductSupplyUseCase {

  private final OwnedProductLoader ownedProductLoader;
  private final SaveProductPort saveProductPort;
  private final LoadSubscriptionsByProductPort loadSubscriptionsByProductPort;
  private final SaveSubscriptionPort saveSubscriptionPort;

  @Override
  public void stopSupply(ProductId productId) {
    Product product = ownedProductLoader.load(productId);
    product.stopSupply();
    saveProductPort.save(product);

    for (Subscription subscription : loadSubscriptionsByProductPort.loadNotCancelled(productId)) {
      subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
      saveSubscriptionPort.save(subscription);
    }
  }
}
