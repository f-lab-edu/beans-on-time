package com.bluetoya.beansontime.refund.adapter.in.scheduling;

import com.bluetoya.beansontime.refund.application.port.in.ReconcileRefundsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "refund.reconciliation.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RefundReconciliationScheduler {
  private final ReconcileRefundsUseCase useCase;

  @Scheduled(
      fixedDelayString = "${refund.reconciliation.delay-ms:30000}",
      initialDelayString = "${refund.reconciliation.delay-ms:30000}")
  public void reconcile() {
    useCase.reconcile();
  }
}
