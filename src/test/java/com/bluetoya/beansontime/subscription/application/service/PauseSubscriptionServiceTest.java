package com.bluetoya.beansontime.subscription.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Cycle;
import com.bluetoya.beansontime.subscription.domain.CycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionPeriod;
import com.bluetoya.beansontime.subscription.domain.SubscriptionStatus;
import com.bluetoya.beansontime.subscription.domain.exception.InvalidSubscriptionPausePeriodException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PauseSubscriptionServiceTest {

  @Test
  void pausesOwnedSubscriptionAtCurrentKstTime() {
    Subscription subscription =
        new Subscription(
            new CustomerId(1),
            new ProductId(10),
            new Cycle(CycleUnit.ONE_MONTH, 1),
            LocalDate.of(2026, 9, 1));
    SubscriptionPeriod currentPeriod = subscription.getCurrentPeriod();
    LocalDate nextBillingDate = subscription.getNextBillingDate();
    LoadSubscriptionPort loadPort = mock(LoadSubscriptionPort.class);
    SaveSubscriptionPort savePort = mock(SaveSubscriptionPort.class);
    when(loadPort.load(subscription.getId())).thenReturn(Optional.of(subscription));
    Clock clock = Clock.fixed(Instant.parse("2026-09-10T05:30:00Z"), ZoneId.of("Asia/Seoul"));
    PauseSubscriptionService service =
        new PauseSubscriptionService(new OwnedSubscriptionLoader(loadPort), savePort, clock);

    service.pause(new PauseSubscriptionCommand(subscription.getId(), LocalDate.of(2026, 9, 20)));

    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.PAUSED);
    assertThat(subscription.getPausedAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 14, 30));
    assertThat(subscription.getResumeDate()).isEqualTo(LocalDate.of(2026, 9, 21));
    assertThat(subscription.getNextBillingDate()).isEqualTo(nextBillingDate);
    assertThat(subscription.getCurrentPeriod()).isEqualTo(currentPeriod);
    assertThat(subscription.isExecutionBlocked()).isTrue();
    verify(savePort).save(subscription);
  }

  @Test
  void rejectsPauseUntilDateBeforeCurrentKstBusinessDate() {
    Subscription subscription =
        new Subscription(
            new CustomerId(1),
            new ProductId(10),
            new Cycle(CycleUnit.ONE_MONTH, 1),
            LocalDate.of(2026, 9, 1));
    LoadSubscriptionPort loadPort = mock(LoadSubscriptionPort.class);
    SaveSubscriptionPort savePort = mock(SaveSubscriptionPort.class);
    when(loadPort.load(subscription.getId())).thenReturn(Optional.of(subscription));
    Clock clock = Clock.fixed(Instant.parse("2026-09-10T05:30:00Z"), ZoneId.of("Asia/Seoul"));
    PauseSubscriptionService service =
        new PauseSubscriptionService(new OwnedSubscriptionLoader(loadPort), savePort, clock);

    assertThatThrownBy(
            () ->
                service.pause(
                    new PauseSubscriptionCommand(subscription.getId(), LocalDate.of(2026, 9, 9))))
        .isInstanceOf(InvalidSubscriptionPausePeriodException.class);

    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    verify(savePort, never()).save(subscription);
  }
}
