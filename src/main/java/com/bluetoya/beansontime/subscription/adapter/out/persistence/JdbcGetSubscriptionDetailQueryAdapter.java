package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.port.in.ProductAvailability;
import com.bluetoya.beansontime.product.application.port.in.ProductInfo;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionDetail;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionInfo;
import com.bluetoya.beansontime.subscription.application.port.out.GetSubscriptionDetailQueryPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcGetSubscriptionDetailQueryAdapter implements GetSubscriptionDetailQueryPort {
  private final JdbcClient jdbc;

  @Override
  public SubscriptionDetail get(SubscriptionId id) {
    return jdbc.sql(
            """
        select s.*, p.name as product_name, p.base_price
        from subscriptions s join products p on p.id = s.product_id where s.id = :id
        """)
        .param("id", id.value())
        .query(
            (rs, row) -> {
              var reasons = SubscriptionRowMapper.suspensionReasons(rs);
              String status = rs.getString("lifecycle_status");
              var subscription =
                  new SubscriptionInfo(
                      rs.getString("id"),
                      rs.getLong("customer_id"),
                      rs.getString("delivery_cycle_unit"),
                      rs.getInt("delivery_cycle_interval"),
                      status,
                      reasons,
                      rs.getObject("started_date", LocalDate.class),
                      rs.getObject("current_period_start_date", LocalDate.class),
                      rs.getObject("current_period_end_date", LocalDate.class),
                      rs.getObject("remaining_paid_days", Integer.class),
                      rs.getInt("billing_anchor_day"),
                      rs.getObject("next_billing_date", LocalDate.class),
                      rs.getObject("paused_at", LocalDateTime.class),
                      rs.getObject("scheduled_resume_date", LocalDate.class),
                      !status.equals("ACTIVE") || !reasons.isEmpty());
              return new SubscriptionDetail(
                  subscription,
                  new ProductInfo(
                      ProductAvailability.AVAILABLE,
                      rs.getLong("product_id"),
                      rs.getString("product_name"),
                      rs.getInt("base_price")));
            })
        .optional()
        .orElseThrow(() -> new SubscriptionNotFoundException("조회할 구독이 존재하지 않습니다."));
  }
}
