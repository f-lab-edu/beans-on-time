package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.subscription.application.port.out.WithdrawalStore;
import com.bluetoya.beansontime.subscription.domain.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("in-memory")
public class InMemoryWithdrawalAdapter implements WithdrawalStore {
  private final Map<SubscriptionId, Withdrawal> values = new ConcurrentHashMap<>();

  public Optional<Withdrawal> find(SubscriptionId id) {
    return Optional.ofNullable(values.get(id));
  }

  public void saveNew(Withdrawal w) {
    if (values.putIfAbsent(w.subscriptionId(), w) != null)
      throw new IllegalStateException("철회 판단은 변경할 수 없습니다.");
  }
}
