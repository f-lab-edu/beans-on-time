package com.bluetoya.beansontime.billing.application.port.out;

import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.function.Supplier;

/** 동일 구독의 청구 준비, 결제 시작과 결과 반영을 서로 배제하는 실행 경계다. */
public interface BillingExecutionPort {
  <T> T execute(SubscriptionId subscriptionId, Supplier<T> action);
}
