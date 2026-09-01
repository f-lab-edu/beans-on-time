package com.bluetoya.beansontime.product.application.service;

import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;

import com.bluetoya.beansontime.product.application.port.in.DiscontinueProductUseCase;
import com.bluetoya.beansontime.product.application.port.in.OwnedProductLoader;
import com.bluetoya.beansontime.product.application.port.in.ResumeProductSupplyUseCase;
import com.bluetoya.beansontime.product.application.port.in.StopProductSupplyUseCase;
import com.bluetoya.beansontime.product.application.port.out.SaveProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionsByProductPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangeProductSupplyService
    implements StopProductSupplyUseCase, ResumeProductSupplyUseCase, DiscontinueProductUseCase {

  private final OwnedProductLoader ownedProductLoader;
  private final SaveProductPort saveProductPort;
  private final LoadSubscriptionsByProductPort loadSubscriptionsByProductPort;
  private final SaveSubscriptionPort saveSubscriptionPort;

  @Override
  public void stopSupply(ProductId productId) {
    Product product = ownedProductLoader.load(productId);
    product.stopSupply();
    saveProductPort.save(product);
    updateSubscriptions(
        productId, subscription -> subscription.addSuspensionReason(PRODUCT_UNAVAILABLE));
  }

  @Override
  public void resumeSupply(ProductId productId) {
    Product product = ownedProductLoader.load(productId);
    product.resumeSupply();
    saveProductPort.save(product);
    updateSubscriptions(
        productId, subscription -> subscription.removeSuspensionReason(PRODUCT_UNAVAILABLE));
  }

  @Override
  public void discontinue(ProductId productId) {
    Product product = ownedProductLoader.load(productId);
    product.discontinue();
    saveProductPort.save(product);
    updateSubscriptions(
        productId, subscription -> subscription.addSuspensionReason(PRODUCT_UNAVAILABLE));
  }

  private void updateSubscriptions(ProductId productId, Consumer<Subscription> update) {
    for (Subscription subscription : loadSubscriptionsByProductPort.loadNotCancelled(productId)) {
      update.accept(subscription);
      saveSubscriptionPort.save(subscription);
    }
  }
}
