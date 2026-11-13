package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.subscription.application.port.out.FindDueAutoResumeSubscriptionsPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcFindDueAutoResumeSubscriptionsAdapter
    implements FindDueAutoResumeSubscriptionsPort {
  private final JdbcClient jdbc;

  @Override
  public List<SubscriptionId> findDue(LocalDate businessDate, SubscriptionId afterId, int limit) {
    return jdbc.sql(
            """
        select id from subscriptions
        where lifecycle_status = 'PAUSED' and remaining_paid_days > 0
          and scheduled_resume_date <= :date
          and (cast(:after as uuid) is null or id > cast(:after as uuid))
        order by id limit :limit
        """)
        .param("date", businessDate)
        .param("after", afterId == null ? null : afterId.value())
        .param("limit", limit)
        .query((rs, row) -> new SubscriptionId(rs.getObject("id", UUID.class)))
        .list();
  }
}
