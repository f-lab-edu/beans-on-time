package com.bluetoya.beansontime.billing.adapter.out.persistence;

import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.billing.domain.BillingStatus;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryBillingRepository {
  private final Map<BillingId, Billing> billings = new ConcurrentHashMap<>();

  public void save(Billing billing) {
    billings.put(billing.getId(), billing);
  }

  public Optional<Billing> findById(BillingId billingId) {
    return Optional.ofNullable(billings.get(billingId));
  }

  public Optional<Billing> findPendingBySubscriptionId(SubscriptionId subscriptionId) {
    return billings.values().stream()
        .filter(billing -> billing.getSubscriptionId().equals(subscriptionId))
        .filter(billing -> billing.getStatus() == BillingStatus.PENDING)
        .findFirst();
  }
}
