package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.customer.domain.CustomerId;

public interface CurrentCustomerProvider {
    CustomerId getCurrentCustomerId();
}
