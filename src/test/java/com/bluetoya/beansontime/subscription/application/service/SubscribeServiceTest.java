package com.bluetoya.beansontime.subscription.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.application.port.in.SubscribeCommand;
import com.bluetoya.beansontime.subscription.application.port.out.CurrentCustomerProvider;
import com.bluetoya.beansontime.subscription.application.port.out.ExistsSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Cycle;
import com.bluetoya.beansontime.subscription.domain.CycleUnit;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscribeServiceTest {

  @Test
  void initializesSubscriptionUsingCurrentKstBusinessDate() {
    SaveSubscriptionPort savePort = mock(SaveSubscriptionPort.class);
    ExistsSubscriptionPort existsPort = mock(ExistsSubscriptionPort.class);
    CurrentCustomerProvider customerProvider = mock(CurrentCustomerProvider.class);
    Clock clock = Clock.fixed(Instant.parse("2026-01-30T15:00:00Z"), ZoneId.of("Asia/Seoul"));
    when(customerProvider.getCurrentCustomerId()).thenReturn(new CustomerId(1));
    when(existsPort.isExists(any(), any())).thenReturn(false);
    SubscribeService service = new SubscribeService(savePort, existsPort, customerProvider, clock);

    service.subscribe(new SubscribeCommand(new ProductId(10), new Cycle(CycleUnit.ONE_MONTH, 1)));

    ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
    verify(savePort).save(captor.capture());
    Subscription saved = captor.getValue();
    assertThat(saved.getStartedDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    assertThat(saved.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
  }
}
