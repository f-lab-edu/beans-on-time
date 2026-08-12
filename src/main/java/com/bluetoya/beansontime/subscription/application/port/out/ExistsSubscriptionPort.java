package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.subscription.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.ProductId;

public interface ExistsSubscriptionPort {
    boolean isExists(CustomerId customerId, ProductId productId);
}
