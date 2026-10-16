package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.payment.application.port.out.PaymentApprovalPort;
import com.bluetoya.beansontime.payment.domain.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("toss-test & !in-memory")
@RequiredArgsConstructor
public class JdbcPaymentApprovalAdapter implements PaymentApprovalPort {
  private final JdbcClient jdbc;

  public Optional<PaymentApproval> load(PaymentId id) {
    return jdbc.sql("select * from payment_approvals where payment_id = :id")
        .param("id", id.value())
        .query(
            (rs, row) ->
                new PaymentApproval(
                    id,
                    rs.getString("transaction_id"),
                    rs.getObject("approved_at", LocalDateTime.class),
                    PaymentApproval.Phase.valueOf(rs.getString("phase")),
                    rs.getObject("application_started_at", LocalDateTime.class),
                    rs.getString("cancel_key"),
                    rs.getObject("cancel_requested_at", LocalDateTime.class),
                    rs.getString("cancel_transaction_id"),
                    rs.getObject("cancelled_at", LocalDateTime.class)))
        .optional();
  }

  public void saveNew(PaymentApproval a) {
    jdbc.sql(
            "insert into payment_approvals(payment_id, transaction_id, approved_at, phase) values (:id, :transaction, :at, 'READY')")
        .param("id", a.paymentId().value())
        .param("transaction", a.transactionId())
        .param("at", time(a.approvedAt()))
        .update();
  }

  public void save(PaymentApproval a) {
    int count =
        jdbc.sql(
                """
        update payment_approvals set phase = :phase, application_started_at = :started,
        cancel_key = :key, cancel_requested_at = :requested, cancel_transaction_id = :transaction, cancelled_at = :at
        where payment_id = :id
        """)
            .param("id", a.paymentId().value())
            .param("phase", a.phase().name())
            .param("started", time(a.applicationStartedAt()))
            .param("key", a.cancelKey())
            .param("requested", time(a.cancelRequestedAt()))
            .param("transaction", a.cancelTransactionId())
            .param("at", time(a.cancelledAt()))
            .update();
    if (count != 1) throw new IllegalStateException("저장할 승인 증거가 없습니다.");
  }

  private LocalDateTime time(LocalDateTime at) {
    return at == null ? null : at.truncatedTo(ChronoUnit.MICROS);
  }
}
