package com.bluetoya.beansontime.refund.application.service;

import static org.mockito.Mockito.*;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.refund.application.port.out.RefundGateway;
import com.bluetoya.beansontime.refund.application.port.out.RefundStore;
import com.bluetoya.beansontime.refund.domain.Refund;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReconcileRefundsServiceTest {
  @Test
  void expiredRefundIsExcludedFromRepeatedAutomaticGatewayCalls() {
    var at = LocalDateTime.of(2026, 10, 3, 10, 0);
    var id = new PaymentId(1);
    var refund = Refund.request(id, new Money(10000), "approved_tx", at, at);
    var refunds = mock(RefundStore.class);
    var processor = mock(RefundProcessor.class);
    var gateway = mock(RefundGateway.class);
    when(refunds.findPending()).thenReturn(List.of(id));
    when(refunds.find(id)).thenReturn(Optional.of(refund));
    when(gateway.canAutomaticallyReconcile(refund, at.plusDays(15))).thenReturn(false);
    var service =
        new ReconcileRefundsService(
            refunds,
            processor,
            gateway,
            Clock.fixed(
                at.plusDays(15).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")));

    service.reconcile();
    service.reconcile();

    verifyNoInteractions(processor);
  }
}
