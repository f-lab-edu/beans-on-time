package com.bluetoya.beansontime.billing.adapter.out.persistence;

import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SubscriptionExecutionPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 청구·결제·구독 갱신을 동일한 상품 잠금과 DB 트랜잭션에 참여시킨다. */
@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class SubscriptionBillingExecutionAdapter implements BillingExecutionPort {
  private final SubscriptionExecutionPort subscriptionExecutionPort;

  public <T> T execute(SubscriptionId id, Supplier<T> action) {
    return subscriptionExecutionPort.execute(id, action);
  }
}
