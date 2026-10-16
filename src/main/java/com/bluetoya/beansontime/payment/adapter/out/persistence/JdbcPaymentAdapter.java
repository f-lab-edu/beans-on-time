package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.exception.PaymentNotFoundException;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.payment.domain.PaymentAuthorization;
import com.bluetoya.beansontime.product.domain.Money;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcPaymentAdapter
    implements SavePaymentPort, LoadPaymentPort, FindProcessingPaymentPort {
  private final JdbcClient jdbc;

  @Override
  public void saveNew(Payment payment) {
    jdbc.sql(
            """
        insert into payments (id, billing_id, amount, status, transaction_id, attempted_at)
        values (:id, :billing, :amount, :status, :transaction, :attempted)
        """)
        .param("id", payment.getId().value())
        .param("billing", payment.getBillingId().value())
        .param("amount", payment.getAmount().price())
        .param("status", payment.getStatus().name())
        .param("transaction", payment.getTransactionId())
        .param("attempted", payment.getAttemptedAt().truncatedTo(ChronoUnit.MICROS))
        .update();
  }

  @Override
  public void saveAuthorized(Payment payment, PaymentAuthorization authorization) {
    saveNew(payment);
    int bound =
        jdbc.sql(
                """
        update payment_checkouts set payment_id = :payment, payment_key = :key
        where order_id = :order and billing_id = :billing and payment_id is null
        """)
            .param("payment", payment.getId().value())
            .param("key", authorization.paymentKey())
            .param("order", authorization.orderId())
            .param("billing", payment.getBillingId().value())
            .update();
    if (bound != 1)
      throw new com.bluetoya.beansontime.billing.application.exception
          .ReactivationBillingNotAllowedException("이 청구의 사용 가능한 결제창 인증 요청이 아닙니다.");
  }

  @Override
  public void save(Payment payment) {
    int updated =
        jdbc.sql(
                "update payments set status = :status, transaction_id = :transaction where id = :id")
            .param("status", payment.getStatus().name())
            .param("transaction", payment.getTransactionId())
            .param("id", payment.getId().value())
            .update();
    if (updated != 1) throw new PaymentNotFoundException("저장할 결제가 존재하지 않습니다.");
  }

  @Override
  public Optional<Payment> load(PaymentId id) {
    return jdbc.sql("select * from payments where id = :id")
        .param("id", id.value())
        .query(this::map)
        .optional();
  }

  @Override
  public Optional<Payment> findProcessing(BillingId id) {
    return jdbc.sql(
            "select * from payments where billing_id = :id and status in ('PROCESSING', 'CANCEL_PENDING')")
        .param("id", id.value())
        .query(this::map)
        .optional();
  }

  @Override
  public List<PaymentId> findProcessingIds() {
    return jdbc.sql(
            "select id from payments where status in ('PROCESSING', 'CANCEL_PENDING') order by attempted_at, id")
        .query((rs, row) -> new PaymentId(rs.getLong("id")))
        .list();
  }

  private Payment map(ResultSet rs, int row) throws SQLException {
    return Payment.restore(
        new PaymentId(rs.getLong("id")),
        new BillingId(rs.getLong("billing_id")),
        new Money(rs.getInt("amount")),
        PaymentStatus.valueOf(rs.getString("status")),
        rs.getString("transaction_id"),
        rs.getObject("attempted_at", LocalDateTime.class));
  }
}
