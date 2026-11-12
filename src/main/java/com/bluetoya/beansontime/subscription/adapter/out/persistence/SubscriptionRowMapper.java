package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.domain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

final class SubscriptionRowMapper implements RowMapper<Subscription> {
  @Override
  public Subscription mapRow(ResultSet rs, int row) throws SQLException {
    LocalDate periodStart = rs.getObject("current_period_start_date", LocalDate.class);
    LocalDate periodEnd = rs.getObject("current_period_end_date", LocalDate.class);
    return Subscription.restore(
        new SubscriptionId(rs.getObject("id", UUID.class)),
        new CustomerId(rs.getLong("customer_id")),
        new ProductId(rs.getLong("product_id")),
        new DeliveryCycle(
            DeliveryCycleUnit.valueOf(rs.getString("delivery_cycle_unit")),
            rs.getInt("delivery_cycle_interval")),
        rs.getObject("started_date", LocalDate.class),
        new BillingAnchorDay(rs.getInt("billing_anchor_day")),
        periodStart == null && periodEnd == null
            ? null
            : new SubscriptionPeriod(periodStart, periodEnd),
        rs.getObject("remaining_paid_days", Integer.class),
        rs.getObject("next_billing_date", LocalDate.class),
        rs.getObject("paused_at", LocalDateTime.class),
        rs.getObject("scheduled_resume_date", LocalDate.class),
        SubscriptionStatus.valueOf(rs.getString("lifecycle_status")),
        suspensionReasons(rs),
        rs.getObject("withdrawn_at", LocalDateTime.class));
  }

  static Set<SubscriptionSuspensionReason> suspensionReasons(ResultSet rs) throws SQLException {
    var result = EnumSet.noneOf(SubscriptionSuspensionReason.class);
    var array = rs.getArray("suspension_reasons");
    try {
      for (String value : (String[]) array.getArray()) {
        result.add(SubscriptionSuspensionReason.valueOf(value));
      }
    } finally {
      array.free();
    }
    return result;
  }
}
