package com.bluetoya.beansontime.refund.adapter.out.persistence;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.refund.application.port.out.RefundStore;
import com.bluetoya.beansontime.refund.domain.Refund;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcRefundAdapter implements RefundStore {
  private final JdbcClient jdbc;

  public void saveNew(Refund r) {
    jdbc.sql(
            "insert into refunds(id,payment_id,amount,approval_transaction_id,approved_at,requested_at) values (:id,:payment,:amount,:approval,:approved,:requested)")
        .param("id", r.id())
        .param("payment", r.paymentId().value())
        .param("amount", r.amount().price())
        .param("approval", r.approvalTransactionId())
        .param("approved", r.approvedAt().truncatedTo(ChronoUnit.MICROS))
        .param("requested", r.requestedAt().truncatedTo(ChronoUnit.MICROS))
        .update();
  }

  public void save(Refund r) {
    int count =
        jdbc.sql(
                "update refunds set transaction_id=:transaction,completed_at=:completed where id=:id")
            .param("id", r.id())
            .param("transaction", r.transactionId())
            .param(
                "completed",
                r.completedAt() == null ? null : r.completedAt().truncatedTo(ChronoUnit.MICROS))
            .update();
    if (count != 1) throw new IllegalStateException("저장할 환불 요청이 없습니다.");
  }

  public Optional<Refund> find(PaymentId id) {
    return jdbc.sql("select * from refunds where payment_id=:id")
        .param("id", id.value())
        .query(
            (rs, row) ->
                new Refund(
                    rs.getObject("id", UUID.class),
                    new PaymentId(rs.getLong("payment_id")),
                    new Money(rs.getInt("amount")),
                    rs.getString("approval_transaction_id"),
                    rs.getObject("approved_at", LocalDateTime.class),
                    rs.getObject("requested_at", LocalDateTime.class),
                    rs.getString("transaction_id"),
                    rs.getObject("completed_at", LocalDateTime.class)))
        .optional();
  }

  public List<Refund> findBySubscription(
      com.bluetoya.beansontime.subscription.domain.SubscriptionId id) {
    return jdbc
        .sql(
            "select r.payment_id from refunds r join payments p on p.id=r.payment_id join billings b on b.id=p.billing_id where b.subscription_id=:id order by r.requested_at,r.id")
        .param("id", id.value())
        .query((rs, row) -> new PaymentId(rs.getLong("payment_id")))
        .list()
        .stream()
        .map(payment -> find(payment).orElseThrow())
        .toList();
  }

  public List<PaymentId> findPending() {
    return jdbc.sql(
            "select payment_id from refunds where completed_at is null order by requested_at,id")
        .query((rs, row) -> new PaymentId(rs.getLong("payment_id")))
        .list();
  }
}
