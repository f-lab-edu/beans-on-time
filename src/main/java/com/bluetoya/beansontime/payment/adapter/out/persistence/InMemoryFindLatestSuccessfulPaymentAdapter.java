package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.billing.adapter.out.persistence.InMemoryBillingRepository;
import com.bluetoya.beansontime.payment.application.port.out.FindLatestSuccessfulPaymentPort;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("in-memory")
@RequiredArgsConstructor
public class InMemoryFindLatestSuccessfulPaymentAdapter implements FindLatestSuccessfulPaymentPort {
  private final InMemoryPaymentRepository payments;
  private final InMemoryBillingRepository billings;

  public Optional<Payment> findLatestSuccess(
      SubscriptionId id, java.time.LocalDateTime requestedAt) {
    return payments.findAll().stream()
        .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
        .filter(
            p -> billings.findById(p.getBillingId()).orElseThrow().getSubscriptionId().equals(id))
        .filter(p -> p.getApprovedAt() == null || !p.getApprovedAt().isAfter(requestedAt))
        .sorted(
            Comparator.comparing(
                    Payment::getApprovedAt, Comparator.nullsFirst(Comparator.reverseOrder()))
                .thenComparing(p -> p.getId().value(), Comparator.reverseOrder()))
        .findFirst();
  }

  public java.util.List<Payment> findApprovedAfterRequest(
      SubscriptionId id, java.time.LocalDateTime requestedAt) {
    return payments.findAll().stream()
        .filter(
            p ->
                p.getStatus() == PaymentStatus.SUCCESS
                    && p.getApprovedAt() != null
                    && p.getApprovedAt().isAfter(requestedAt))
        .filter(
            p -> billings.findById(p.getBillingId()).orElseThrow().getSubscriptionId().equals(id))
        .toList();
  }
}
