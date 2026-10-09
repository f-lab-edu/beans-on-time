package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.out.*;
import com.bluetoya.beansontime.subscription.domain.*;
import java.sql.Types;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcSubscriptionAdapter
    implements SaveSubscriptionPort,
        LoadSubscriptionPort,
        ExistsSubscriptionPort,
        LoadSubscriptionsByProductPort {
  private final JdbcClient jdbc;
  private final SubscriptionRowMapper mapper = new SubscriptionRowMapper();

  @Override
  public void saveNew(Subscription subscription) {
    stateParameters(
            jdbc.sql(
                """
        insert into subscriptions (id, customer_id, product_id, delivery_cycle_unit,
          delivery_cycle_interval, started_date, billing_anchor_day, current_period_start_date,
          current_period_end_date, remaining_paid_days, next_billing_date, paused_at,
          scheduled_resume_date, lifecycle_status, suspension_reasons, withdrawn_at)
        values (:id, :customerId, :productId, :unit, :interval, :startedDate, :anchor,
          :periodStart, :periodEnd, :remaining, :nextBillingDate, :pausedAt,
          :scheduledResumeDate, :status, string_to_array(:reasons, ','), :withdrawnAt)
        """),
            subscription)
        .param("customerId", subscription.getCustomerId().value())
        .param("productId", subscription.getProductId().id())
        .param("unit", subscription.getDeliveryCycle().unit().name())
        .param("interval", subscription.getDeliveryCycle().interval())
        .param("startedDate", subscription.getStartedDate())
        .update();
  }

  @Override
  public void save(Subscription subscription) {
    int updated =
        stateParameters(
                jdbc.sql(
                    """
        update subscriptions set billing_anchor_day = :anchor,
          current_period_start_date = :periodStart, current_period_end_date = :periodEnd,
          remaining_paid_days = :remaining, next_billing_date = :nextBillingDate,
          paused_at = :pausedAt, scheduled_resume_date = :scheduledResumeDate,
          lifecycle_status = :status, suspension_reasons = string_to_array(:reasons, ','), withdrawn_at = :withdrawnAt
        where id = :id
        """),
                subscription)
            .update();
    if (updated != 1) throw new SubscriptionNotFoundException("저장할 구독이 존재하지 않습니다.");
  }

  private JdbcClient.StatementSpec stateParameters(JdbcClient.StatementSpec sql, Subscription s) {
    var period = s.getCurrentPeriod();
    // PostgreSQL의 마이크로초 반올림으로 일시정지 업무 날짜가 다음 날로 넘어가지 않게 한다.
    var pausedAt = s.getPausedAt() == null ? null : s.getPausedAt().truncatedTo(ChronoUnit.MICROS);
    return sql.param("id", s.getId().value())
        .param("anchor", s.getBillingAnchorDay().value())
        .param("periodStart", period == null ? null : period.startDate(), Types.DATE)
        .param("periodEnd", period == null ? null : period.endDate(), Types.DATE)
        .param("remaining", s.getRemainingPaidDays(), Types.INTEGER)
        .param("nextBillingDate", s.getNextBillingDate(), Types.DATE)
        .param("pausedAt", pausedAt, Types.TIMESTAMP)
        .param("scheduledResumeDate", s.getScheduledResumeDate(), Types.DATE)
        .param(
            "withdrawnAt",
            s.getWithdrawnAt() == null ? null : s.getWithdrawnAt().truncatedTo(ChronoUnit.MICROS),
            Types.TIMESTAMP)
        .param("status", s.getLifecycleStatus().name())
        .param(
            "reasons",
            s.getSuspensionReasons().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(",")));
  }

  @Override
  public Optional<Subscription> load(SubscriptionId id) {
    return jdbc.sql("select * from subscriptions where id = :id")
        .param("id", id.value())
        .query(mapper)
        .optional();
  }

  @Override
  public boolean isExists(CustomerId customerId, ProductId productId) {
    return jdbc.sql(
            """
        select exists(select 1 from subscriptions
          where customer_id = :customerId and product_id = :productId and lifecycle_status <> 'CANCELLED')
        """)
        .param("customerId", customerId.value())
        .param("productId", productId.id())
        .query(Boolean.class)
        .single();
  }

  @Override
  public List<Subscription> loadNotCancelled(ProductId productId) {
    return jdbc.sql(
            "select * from subscriptions where product_id = :id and lifecycle_status <> 'CANCELLED' order by id")
        .param("id", productId.id())
        .query(mapper)
        .list();
  }
}
