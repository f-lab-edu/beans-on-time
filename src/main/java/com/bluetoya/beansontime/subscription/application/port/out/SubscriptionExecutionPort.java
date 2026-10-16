package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.function.Supplier;

/** 대상의 최신 상태 로드부터 저장까지 다른 변경과 배제하여 실행한다. */
public interface SubscriptionExecutionPort {
  <T> T execute(SubscriptionId id, Supplier<T> action);
}
