package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.security.application.CurrentCustomerProvider;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.subscription.application.port.in.FindSubscriptionQuery;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.application.port.out.FindSubscriptionQueryPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindSubscriptionService implements FindSubscriptionQuery {
  private final FindSubscriptionQueryPort findSubscriptionQueryPort;
  private final CurrentCustomerProvider currentCustomerProvider;

  @Override
  public SubscriptionQueryResult find(SubscriptionId subscriptionId) {
    SubscriptionQueryResult result = findSubscriptionQueryPort.find(subscriptionId);

    CustomerId customerId = currentCustomerProvider.getCurrentCustomerId();

    if (result.customerId() != customerId.id()) {
      throw new IllegalArgumentException("고객이 신청한 구독이 아닙니다.");
    }

    return result;
  }
}
