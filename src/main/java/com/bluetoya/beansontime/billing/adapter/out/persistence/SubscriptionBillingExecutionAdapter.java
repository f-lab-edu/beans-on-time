package com.bluetoya.beansontime.billing.adapter.out.persistence;

import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SubscriptionExecutionPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 청구·결제·구독 갱신을 동일한 상품 실행 경계에 참여시킨다. PostgreSQL에서는 DB 트랜잭션도 공유한다. */
@Component
@RequiredArgsConstructor
public class SubscriptionBillingExecutionAdapter implements BillingExecutionPort {
  private final SubscriptionExecutionPort subscriptionExecutionPort;

  public <T> T execute(SubscriptionId id, Supplier<T> action) {
    return subscriptionExecutionPort.execute(id, action);
  }
}
