package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.subscription.application.exception.DuplicateSubscriptionException;
import com.bluetoya.beansontime.subscription.application.exception.ProductNotSubscribableException;
import com.bluetoya.beansontime.subscription.application.port.in.SubscribeCommand;
import com.bluetoya.beansontime.subscription.application.port.in.SubscribeUseCase;
import com.bluetoya.beansontime.subscription.application.port.out.CurrentCustomerProvider;
import com.bluetoya.beansontime.subscription.application.port.out.ExistsSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscribeService implements SubscribeUseCase {
  private final SaveSubscriptionPort saveSubscriptionPort;
  private final ExistsSubscriptionPort existsSubscriptionPort;
  private final LoadProductPort loadProductPort;
  private final CurrentCustomerProvider currentCustomerProvider;
  private final Clock clock;

  @Override
  public SubscriptionId subscribe(SubscribeCommand command) {
    CustomerId customerId = currentCustomerProvider.getCurrentCustomerId();
    Product product =
        loadProductPort
            .load(command.productId())
            .orElseThrow(() -> new ProductNotFoundException("구독할 상품을 찾을 수 없습니다."));

    if (!product.isSubscribable()) {
      throw new ProductNotSubscribableException("현재 공급 상태에서는 신규 구독할 수 없습니다.");
    }

    if (existsSubscriptionPort.isExists(customerId, command.productId())) {
      throw new DuplicateSubscriptionException("중복 구독 불가");
    }

    Subscription subscription =
        new Subscription(
            customerId, command.productId(), command.deliveryCycle(), LocalDate.now(clock));
    saveSubscriptionPort.save(subscription);
    return subscription.getId();
  }
}
