package com.bluetoya.beansontime.subscription.adapter.out.persistence;

import com.bluetoya.beansontime.subscription.application.port.out.FindDueSubscriptionsPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Profile("!in-memory")
@RequiredArgsConstructor
public class JdbcFindDueSubscriptionsAdapter implements FindDueSubscriptionsPort {
  private final JdbcClient jdbc;

  public List<DueSubscription> findDue(LocalDate businessDate, SubscriptionId afterId, int limit) {
    return jdbc.sql(
            """
        select id, next_billing_date from subscriptions
        where lifecycle_status = 'ACTIVE' and cardinality(suspension_reasons) = 0
          and next_billing_date <= :date
          and (cast(:after as uuid) is null or id > cast(:after as uuid))
        order by id limit :limit
        """)
        .param("date", businessDate)
        .param("after", afterId == null ? null : afterId.value())
        .param("limit", limit)
        .query(
            (rs, row) ->
                new DueSubscription(
                    new SubscriptionId(rs.getObject("id", UUID.class)),
                    rs.getObject("next_billing_date", LocalDate.class)))
        .list();
  }
}
