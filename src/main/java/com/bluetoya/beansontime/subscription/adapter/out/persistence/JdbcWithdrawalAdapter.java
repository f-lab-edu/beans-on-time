package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.subscription.application.port.out.WithdrawalStore;
import com.bluetoya.beansontime.subscription.domain.*;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcWithdrawalAdapter implements WithdrawalStore {
  private final JdbcClient jdbc;

  public Optional<Withdrawal> find(SubscriptionId id) {
    return jdbc.sql("select * from subscription_withdrawals where subscription_id=:id")
        .param("id", id.value())
        .query(
            (rs, row) -> {
              Long payment = rs.getObject("payment_id", Long.class);
              return new Withdrawal(
                  id,
                  rs.getObject("requested_at", LocalDateTime.class),
                  payment == null ? null : new PaymentId(payment),
                  Withdrawal.Decision.valueOf(rs.getString("decision")));
            })
        .optional();
  }

  public void saveNew(Withdrawal w) {
    jdbc.sql(
            "insert into subscription_withdrawals(subscription_id,requested_at,payment_id,decision) values (:id,:at,:payment,:decision)")
        .param("id", w.subscriptionId().value())
        .param("at", w.requestedAt())
        .param("payment", w.paymentId() == null ? null : w.paymentId().value())
        .param("decision", w.decision().name())
        .update();
  }
}
