package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.payment.domain.PaymentStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPaymentRepository {
  private final Map<PaymentId, Payment> payments = new ConcurrentHashMap<>();

  public void save(Payment payment) {
    payments.put(payment.getId(), payment);
  }

  public Optional<Payment> findById(PaymentId id) {
    return Optional.ofNullable(payments.get(id));
  }

  public Optional<Payment> findProcessing(BillingId id) {
    return payments.values().stream()
        .filter(p -> p.getBillingId().equals(id))
        .filter(p -> p.getStatus() == PaymentStatus.PROCESSING)
        .findFirst();
  }

  public List<PaymentId> findProcessingIds() {
    return payments.values().stream()
        .filter(p -> p.getStatus() == PaymentStatus.PROCESSING)
        .map(Payment::getId)
        .toList();
  }
}
