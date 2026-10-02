package com.bluetoya.beansontime.billing.adapter.out.persistence;

import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** 단일 프로세스의 실행 배제만 제공한다. DB 트랜잭션이나 롤백을 대체하지 않는다. */
@Component
public class InMemoryBillingExecutionAdapter implements BillingExecutionPort {
  private final ReentrantLock[] locks = new ReentrantLock[256];

  public InMemoryBillingExecutionAdapter() {
    for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
  }

  @Override
  public <T> T execute(SubscriptionId id, Supplier<T> action) {
    ReentrantLock lock = locks[Math.floorMod(id.hashCode(), locks.length)];
    lock.lock();
    try {
      return action.get();
    } finally {
      lock.unlock();
    }
  }
}
