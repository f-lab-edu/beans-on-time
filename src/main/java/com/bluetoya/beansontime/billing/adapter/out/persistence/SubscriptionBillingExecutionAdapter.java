package com.bluetoya.beansontime.billing.adapter.out.persistence;

import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SubscriptionExecutionPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 구독 갱신은 DB 실행 경계에 참여한다. InMemory 청구·결제의 롤백은 보장하지 않는다. */
@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class SubscriptionBillingExecutionAdapter implements BillingExecutionPort {
  private final SubscriptionExecutionPort subscriptionExecutionPort;

  public <T> T execute(SubscriptionId id, Supplier<T> action) {
    return subscriptionExecutionPort.execute(id, action);
  }
}
