package com.bluetoya.beansontime.refund.application.service;

import com.bluetoya.beansontime.refund.application.port.in.ReconcileRefundsUseCase;
import com.bluetoya.beansontime.refund.application.port.out.RefundStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconcileRefundsService implements ReconcileRefundsUseCase {
  private final RefundStore refunds;
  private final RefundProcessor processor;

  public void reconcile() {
    for (var id : refunds.findPending())
      try {
        processor.process(id);
      } catch (RuntimeException e) {
        log.warn("환불 결과 확인 필요: paymentId={}", id.value(), e);
      }
  }
}
