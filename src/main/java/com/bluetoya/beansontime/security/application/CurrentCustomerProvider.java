package com.bluetoya.beansontime.security.application;

import com.bluetoya.beansontime.customer.domain.CustomerId;

public interface CurrentCustomerProvider {
    CustomerId getCurrentCustomerId();
}
