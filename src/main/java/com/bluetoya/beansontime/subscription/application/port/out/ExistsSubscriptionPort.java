package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;

public interface ExistsSubscriptionPort {
  boolean isExists(CustomerId customerId, ProductId productId);
}
