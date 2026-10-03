package com.bluetoya.beansontime.product.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.port.out.ProductExecutionPort;
import com.bluetoya.beansontime.product.domain.ProductId;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("in-memory")
public class InMemoryProductExecutionAdapter implements ProductExecutionPort {
  private final ReentrantLock[] locks = new ReentrantLock[256];

  public InMemoryProductExecutionAdapter() {
    for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
  }

  public <T> T execute(ProductId id, Supplier<T> action) {
    ReentrantLock lock = locks[Math.floorMod(id.hashCode(), locks.length)];
    lock.lock();
    try {
      return action.get();
    } finally {
      lock.unlock();
    }
  }
}
