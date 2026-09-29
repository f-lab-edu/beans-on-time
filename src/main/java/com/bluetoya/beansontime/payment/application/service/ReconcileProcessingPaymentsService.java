package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.payment.application.port.in.ReconcileProcessingPaymentsUseCase;
import com.bluetoya.beansontime.payment.application.port.out.FindProcessingPaymentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconcileProcessingPaymentsService implements ReconcileProcessingPaymentsUseCase {
  private final FindProcessingPaymentPort findProcessingPaymentPort;
  private final PaymentResultResolver paymentResultResolver;

  @Override
  public void reconcileProcessing() {
    for (var paymentId : findProcessingPaymentPort.findProcessingIds()) {
      try {
        paymentResultResolver.resolve(paymentId);
      } catch (RuntimeException exception) {
        log.warn("결제 결과 반영 확인 필요: paymentId={}", paymentId.value(), exception);
      }
    }
  }
}
