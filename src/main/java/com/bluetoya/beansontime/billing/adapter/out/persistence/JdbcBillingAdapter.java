package com.bluetoya.beansontime.billing.adapter.out.persistence;

import com.bluetoya.beansontime.billing.application.exception.BillingNotFoundException;
import com.bluetoya.beansontime.billing.application.port.out.*;
import com.bluetoya.beansontime.billing.domain.*;
import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.*;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcBillingAdapter
    implements SaveBillingPort,
        LoadBillingPort,
        FindPendingBillingPort,
        com.bluetoya.beansontime.billing.application.port.out.FindRecurringBillingPort {
  private final JdbcClient jdbc;

  @Override
  public void saveNew(Billing billing) {
    jdbc.sql(
            """
        insert into billings (id, customer_id, subscription_id, product_id, amount,
                              billing_date, created_at, expires_at, status, purpose)
        values (:id, :customer, :subscription, :product, :amount, :date, :created, :expires, :status, :purpose)
        """)
        .param("id", billing.getId().value())
        .param("customer", billing.getCustomerId().value())
        .param("subscription", billing.getSubscriptionId().value())
        .param("product", billing.getProductId().id())
        .param("amount", billing.getAmount().price())
        .param("date", billing.getBillingDate())
        .param("created", billing.getCreatedAt().truncatedTo(ChronoUnit.MICROS))
        .param(
            "expires",
            billing.getExpiresAt() == null
                ? null
                : billing.getExpiresAt().truncatedTo(ChronoUnit.MICROS))
        .param("purpose", billing.getPurpose().name())
        .param("status", billing.getStatus().name())
        .update();
  }

  @Override
  public void save(Billing billing) {
    int updated =
        jdbc.sql("update billings set status = :status where id = :id")
            .param("status", billing.getStatus().name())
            .param("id", billing.getId().value())
            .update();
    if (updated != 1) throw new BillingNotFoundException("저장할 청구가 존재하지 않습니다.");
  }

  @Override
  public Optional<Billing> load(BillingId id) {
    return jdbc.sql("select * from billings where id = :id")
        .param("id", id.value())
        .query(this::map)
        .optional();
  }

  @Override
  public Optional<Billing> findPending(SubscriptionId id) {
    return jdbc.sql("select * from billings where subscription_id = :id and status = 'PENDING'")
        .param("id", id.value())
        .query(this::map)
        .optional();
  }

  @Override
  public Optional<Billing> findRecurring(SubscriptionId id, LocalDate dueDate) {
    return jdbc.sql(
            "select * from billings where subscription_id = :id and billing_date = :date and purpose = 'RECURRING'")
        .param("id", id.value())
        .param("date", dueDate)
        .query(this::map)
        .optional();
  }

  private Billing map(ResultSet rs, int row) throws SQLException {
    return Billing.restore(
        new BillingId(rs.getLong("id")),
        new CustomerId(rs.getLong("customer_id")),
        new SubscriptionId(rs.getObject("subscription_id", UUID.class)),
        new ProductId(rs.getLong("product_id")),
        new Money(rs.getInt("amount")),
        rs.getObject("billing_date", LocalDate.class),
        rs.getObject("created_at", LocalDateTime.class),
        rs.getObject("expires_at", LocalDateTime.class),
        BillingStatus.valueOf(rs.getString("status")),
        BillingPurpose.valueOf(rs.getString("purpose")));
  }
}
