package com.bluetoya.beansontime.payment.adapter.in.scheduling;

import com.bluetoya.beansontime.payment.application.port.in.ReconcileProcessingPaymentsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "payment.reconciliation.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class PaymentReconciliationScheduler {
  private final ReconcileProcessingPaymentsUseCase reconcileProcessingPaymentsUseCase;

  @Scheduled(
      fixedDelayString = "${payment.reconciliation.delay-ms:30000}",
      initialDelayString = "${payment.reconciliation.delay-ms:30000}")
  public void reconcile() {
    reconcileProcessingPaymentsUseCase.reconcileProcessing();
  }
}
