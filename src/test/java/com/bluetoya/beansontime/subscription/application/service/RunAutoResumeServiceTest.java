package com.bluetoya.beansontime.subscription.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.adapter.out.persistence.InMemoryProductExecutionAdapter;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.adapter.out.persistence.SubscriptionExecutionAdapter;
import com.bluetoya.beansontime.subscription.application.port.out.*;
import com.bluetoya.beansontime.subscription.domain.*;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RunAutoResumeServiceTest {
  @Test
  void skipsStaleCandidateAfterManualResumeWinsTheRace() {
    var today = LocalDate.of(2026, 10, 11);
    var subscription =
        new Subscription(
            new CustomerId(1),
            new ProductId(1),
            new DeliveryCycle(DeliveryCycleUnit.ONE_MONTH, 1),
            LocalDate.of(2026, 10, 1));
    subscription.pause(today.minusDays(1), LocalDateTime.of(2026, 10, 3, 10, 0));
    var candidates = mock(FindDueAutoResumeSubscriptionsPort.class);
    var load = mock(LoadSubscriptionPort.class);
    var save = mock(SaveSubscriptionPort.class);
    when(load.load(subscription.getId())).thenReturn(Optional.of(subscription));
    when(candidates.findDue(today, null, 100))
        .thenAnswer(
            call -> {
              subscription.resume(today);
              return List.of(subscription.getId());
            });
    when(candidates.findDue(today, subscription.getId(), 100)).thenReturn(List.of());
    var service =
        new RunAutoResumeService(
            candidates,
            load,
            save,
            new SubscriptionExecutionAdapter(load, new InMemoryProductExecutionAdapter()),
            Clock.fixed(
                today.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul")));

    service.runDue();

    assertThat(subscription.getLifecycleStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(subscription.getCurrentPeriod().startDate()).isEqualTo(today);
    verify(save, never()).save(any());
  }
}
