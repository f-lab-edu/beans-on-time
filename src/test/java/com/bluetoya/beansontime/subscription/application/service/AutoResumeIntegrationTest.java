package com.bluetoya.beansontime.subscription.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.adapter.out.persistence.JdbcProductAdapter;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.seller.domain.SellerId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.JdbcSubscriptionAdapter;
import com.bluetoya.beansontime.subscription.application.port.in.RunAutoResumeUseCase;
import com.bluetoya.beansontime.subscription.domain.*;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "subscription.auto-resume.enabled=false",
      "billing.recurring.enabled=false",
      "payment.reconciliation.enabled=false",
      "refund.reconciliation.enabled=false"
    })
@Testcontainers
@Import(AutoResumeIntegrationTest.TimeConfig.class)
class AutoResumeIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

  @Autowired RunAutoResumeUseCase autoResume;
  @Autowired JdbcProductAdapter products;
  @Autowired JdbcSubscriptionAdapter subscriptions;
  @Autowired JdbcClient jdbc;
  @Autowired MutableClock clock;

  private static final LocalDate DUE = LocalDate.of(2026, 10, 11);

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    MutableClock clock() {
      return new MutableClock();
    }
  }

  static class MutableClock extends Clock {
    LocalDateTime now = DUE.atTime(10, 0);

    public ZoneId getZone() {
      return ZoneId.of("Asia/Seoul");
    }

    public Clock withZone(ZoneId zone) {
      return Clock.fixed(instant(), zone);
    }

    public Instant instant() {
      return now.atZone(getZone()).toInstant();
    }
  }

  @BeforeEach
  void resetDatabase() {
    clock.now = DUE.atTime(10, 0);
    jdbc.sql("truncate products cascade").update();
  }

  private Subscription subscribe(LocalDate started) {
    var product = new Product(new SellerId(1), "자동 재개 원두", new Money(12000));
    products.saveNew(product);
    var subscription =
        new Subscription(
            new CustomerId(1),
            product.getId(),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            started);
    subscriptions.saveNew(subscription);
    return subscription;
  }

  private Subscription pause(LocalDate started, LocalDateTime at, LocalDate until) {
    var subscription = subscribe(started);
    subscription.pause(until, at);
    subscriptions.save(subscription);
    return subscription;
  }

  @Test
  void resumesDuePaidSubscriptionsAndSkipsFutureAndZeroPaidDays() {
    var due =
        pause(LocalDate.of(2026, 10, 1), LocalDateTime.of(2026, 10, 3, 10, 0), DUE.minusDays(1));
    var future = pause(LocalDate.of(2026, 10, 1), LocalDateTime.of(2026, 10, 3, 10, 0), DUE);
    var zero =
        pause(LocalDate.of(2026, 9, 1), LocalDateTime.of(2026, 9, 30, 10, 0), DUE.minusDays(1));

    autoResume.runDue();

    var resumed = subscriptions.load(due.getId()).orElseThrow();
    assertThat(resumed.getLifecycleStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(resumed.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(DUE, LocalDate.of(2026, 11, 7)));
    assertThat(resumed.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 8));
    assertThat(subscriptions.load(future.getId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.PAUSED);
    assertThat(subscriptions.load(zero.getId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.PAUSED);

    clock.now = DUE.plusDays(1).atTime(10, 0);
    autoResume.runDue();
    assertThat(subscriptions.load(future.getId()).orElseThrow().getCurrentPeriod().startDate())
        .isEqualTo(DUE.plusDays(1));
    assertThat(subscriptions.load(zero.getId()).orElseThrow().getRemainingPaidDays()).isZero();
  }

  @Test
  void retriesAfterFailedSaveUsingTheCurrentKstDate() {
    var paused =
        pause(LocalDate.of(2026, 10, 1), LocalDateTime.of(2026, 10, 3, 10, 0), DUE.minusDays(1));
    jdbc.sql(
            "alter table subscriptions add constraint injected_auto_resume_failure check(lifecycle_status <> 'ACTIVE') not valid")
        .update();
    try {
      autoResume.runDue();
    } finally {
      jdbc.sql("alter table subscriptions drop constraint injected_auto_resume_failure").update();
    }
    assertThat(subscriptions.load(paused.getId()).orElseThrow().getLifecycleStatus())
        .isEqualTo(SubscriptionStatus.PAUSED);

    clock.now = DUE.plusDays(1).atTime(10, 0);
    autoResume.runDue();

    var resumed = subscriptions.load(paused.getId()).orElseThrow();
    assertThat(resumed.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(DUE.plusDays(1), LocalDate.of(2026, 11, 8)));
    assertThat(resumed.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 9));
    assertThat(resumed.getScheduledResumeDate()).isNull();
  }
}
