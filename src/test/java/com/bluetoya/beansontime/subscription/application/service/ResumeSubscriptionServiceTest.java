package com.bluetoya.beansontime.subscription.application.service;

import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.application.port.in.ResumeSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Cycle;
import com.bluetoya.beansontime.subscription.domain.CycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionPeriod;
import com.bluetoya.beansontime.subscription.domain.SubscriptionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResumeSubscriptionServiceTest {

  @Test
  void resumesInsideCurrentPeriodWithoutReplacingPeriodOrBillingDate() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 9, 1));
    SubscriptionPeriod currentPeriod = subscription.getCurrentPeriod();
    LocalDate nextBillingDate = subscription.getNextBillingDate();
    subscription.pause(LocalDate.of(2026, 9, 25), LocalDateTime.of(2026, 9, 10, 14, 30));
    LoadSubscriptionPort loadPort = mock(LoadSubscriptionPort.class);
    SaveSubscriptionPort savePort = mock(SaveSubscriptionPort.class);
    when(loadPort.load(subscription.getId())).thenReturn(Optional.of(subscription));
    Clock clock = Clock.fixed(Instant.parse("2026-09-20T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    ResumeSubscriptionService service =
        new ResumeSubscriptionService(new OwnedSubscriptionLoader(loadPort), savePort, clock);

    service.resume(new ResumeSubscriptionCommand(subscription.getId()));

    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(subscription.getCurrentPeriod()).isEqualTo(currentPeriod);
    assertThat(subscription.getNextBillingDate()).isEqualTo(nextBillingDate);
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getResumeDate()).isNull();
    verify(savePort).save(subscription);
  }

  @Test
  void confirmsNewPeriodAfterCurrentPeriodWithoutRemovingSuspensionReasons() {
    Subscription subscription = subscriptionStartedOn(LocalDate.of(2026, 8, 31));
    subscription.addSuspensionReason(PRODUCT_UNAVAILABLE);
    subscription.pause(LocalDate.of(2026, 10, 20), LocalDateTime.of(2026, 9, 10, 14, 30));
    LoadSubscriptionPort loadPort = mock(LoadSubscriptionPort.class);
    SaveSubscriptionPort savePort = mock(SaveSubscriptionPort.class);
    when(loadPort.load(subscription.getId())).thenReturn(Optional.of(subscription));
    Clock clock = Clock.fixed(Instant.parse("2026-10-10T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    ResumeSubscriptionService service =
        new ResumeSubscriptionService(new OwnedSubscriptionLoader(loadPort), savePort, clock);

    service.resume(new ResumeSubscriptionCommand(subscription.getId()));

    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(subscription.getSuspensionReasons()).containsExactly(PRODUCT_UNAVAILABLE);
    assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 30));
    assertThat(subscription.getCurrentPeriod())
        .isEqualTo(new SubscriptionPeriod(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 11, 29)));
    assertThat(subscription.getPausedAt()).isNull();
    assertThat(subscription.getResumeDate()).isNull();
    assertThat(subscription.isExecutionBlocked()).isTrue();
    verify(savePort).save(subscription);
  }

  private Subscription subscriptionStartedOn(LocalDate startedDate) {
    return new Subscription(
        new CustomerId(1), new ProductId(10), new Cycle(CycleUnit.ONE_MONTH, 1), startedDate);
  }
}
