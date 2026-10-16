package com.bluetoya.beansontime.billing.adapter.out.persistence;

import com.bluetoya.beansontime.billing.application.exception.BillingNotFoundException;
import com.bluetoya.beansontime.billing.application.port.in.BillingCheckoutDetail;
import com.bluetoya.beansontime.billing.application.port.out.GetBillingCheckoutQueryPort;
import com.bluetoya.beansontime.billing.domain.BillingId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcGetBillingCheckoutQueryAdapter implements GetBillingCheckoutQueryPort {
  private final JdbcClient jdbc;

  @Override
  public BillingCheckoutDetail get(BillingId id) {
    return jdbc.sql(
            """
        select b.*, s.lifecycle_status, p.name as product_name
        from billings b join subscriptions s on s.id = b.subscription_id
        join products p on p.id = b.product_id where b.id = :id
        """)
        .param("id", id.value())
        .query(
            (rs, row) ->
                new BillingCheckoutDetail(
                    rs.getLong("customer_id"),
                    new BillingCheckoutDetail.SubscriptionInfo(
                        rs.getString("subscription_id"), rs.getString("lifecycle_status")),
                    new BillingCheckoutDetail.ProductInfo(
                        rs.getLong("product_id"), rs.getString("product_name")),
                    new BillingCheckoutDetail.BillingInfo(
                        rs.getLong("id"),
                        rs.getInt("amount"),
                        rs.getObject("billing_date", LocalDate.class),
                        rs.getString("status"),
                        rs.getObject("expires_at", LocalDateTime.class))))
        .optional()
        .orElseThrow(() -> new BillingNotFoundException("조회할 청구가 존재하지 않습니다."));
  }
}
