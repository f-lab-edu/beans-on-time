package com.bluetoya.beansontime.billing.adapter.out.persistence;

import com.bluetoya.beansontime.billing.application.port.out.FindPendingBillingPort;
import com.bluetoya.beansontime.billing.application.port.out.LoadBillingPort;
import com.bluetoya.beansontime.billing.application.port.out.SaveBillingPort;
import com.bluetoya.beansontime.billing.domain.Billing;
import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryBillingAdapter
    implements SaveBillingPort, LoadBillingPort, FindPendingBillingPort {
  private final InMemoryBillingRepository billingRepository;

  @Override
  public void save(Billing billing) {
    billingRepository.save(billing);
  }

  @Override
  public Optional<Billing> load(BillingId billingId) {
    return billingRepository.findById(billingId);
  }

  @Override
  public Optional<Billing> findPending(SubscriptionId subscriptionId) {
    return billingRepository.findPendingBySubscriptionId(subscriptionId);
  }
}
