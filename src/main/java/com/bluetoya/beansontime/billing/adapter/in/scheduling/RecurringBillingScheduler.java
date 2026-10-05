package com.bluetoya.beansontime.billing.adapter.in.scheduling;

import com.bluetoya.beansontime.billing.application.port.in.RunRecurringBillingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@Profile("!toss-test")
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "billing.recurring.enabled", havingValue = "true")
public class RecurringBillingScheduler {
  private final RunRecurringBillingUseCase useCase;

  @Scheduled(
      fixedDelayString = "${billing.recurring.delay-ms:60000}",
      initialDelayString = "${billing.recurring.delay-ms:60000}")
  public void billDue() {
    useCase.runDue();
  }
}
