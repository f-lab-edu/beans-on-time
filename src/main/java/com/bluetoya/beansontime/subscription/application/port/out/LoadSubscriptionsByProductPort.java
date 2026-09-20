package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.util.List;

public interface LoadSubscriptionsByProductPort {
  List<Subscription> loadNotCancelled(ProductId productId);
}
