package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPaymentRepository {
  private final Map<PaymentId, Payment> payments = new ConcurrentHashMap<>();

  public void save(Payment payment) {
    payments.put(payment.getId(), payment);
  }

  public boolean existsByBillingId(BillingId billingId) {
    return payments.values().stream().anyMatch(payment -> payment.getBillingId().equals(billingId));
  }
}
