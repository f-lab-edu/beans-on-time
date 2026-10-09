package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcFindLatestSuccessfulPaymentAdapter implements FindLatestSuccessfulPaymentPort {
  private final JdbcClient jdbc;
  private final LoadPaymentPort payments;

  public Optional<Payment> findLatestSuccess(
      SubscriptionId id, java.time.LocalDateTime requestedAt) {
    return jdbc.sql(
            "select p.id from payments p join billings b on b.id=p.billing_id where b.subscription_id=:id and p.status='SUCCESS' and (p.approved_at is null or p.approved_at<=:at) order by p.approved_at desc nulls first,p.id desc limit 1")
        .param("id", id.value())
        .param("at", requestedAt)
        .query(Long.class)
        .optional()
        .map(value -> payments.load(new PaymentId(value)).orElseThrow());
  }

  public java.util.List<Payment> findApprovedAfterRequest(
      SubscriptionId id, java.time.LocalDateTime requestedAt) {
    return jdbc
        .sql(
            "select p.id from payments p join billings b on b.id=p.billing_id where b.subscription_id=:id and p.status='SUCCESS' and p.approved_at>:at order by p.approved_at,p.id")
        .param("id", id.value())
        .param("at", requestedAt)
        .query(Long.class)
        .list()
        .stream()
        .map(value -> payments.load(new PaymentId(value)).orElseThrow())
        .toList();
  }
}
