package com.bluetoya.beansontime.refund.application.service;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.refund.application.port.in.ReconcileRefundsUseCase;
import com.bluetoya.beansontime.refund.application.port.out.RefundGateway;
import com.bluetoya.beansontime.refund.application.port.out.RefundStore;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconcileRefundsService implements ReconcileRefundsUseCase {
  private final RefundStore refunds;
  private final RefundProcessor processor;
  private final RefundGateway gateway;
  private final Clock clock;
  private final Set<PaymentId> overdueLogged = ConcurrentHashMap.newKeySet();

  public void reconcile() {
    for (var id : refunds.findPending())
      try {
        var refund = refunds.find(id).orElseThrow();
        if (!gateway.canAutomaticallyReconcile(refund, LocalDateTime.now(clock))) {
          if (overdueLogged.add(id))
            log.warn(
                "환불 자동 조회 기간 경과, 운영 확인 필요: paymentId={}, refundId={}", id.value(), refund.id());
          continue;
        }
        processor.process(id);
      } catch (RuntimeException e) {
        log.warn("환불 결과 확인 필요: paymentId={}", id.value(), e);
      }
  }
}
