package com.bluetoya.beansontime.refund.application.service;

import com.bluetoya.beansontime.billing.application.port.out.*;
import com.bluetoya.beansontime.payment.application.port.out.LoadPaymentPort;
import com.bluetoya.beansontime.payment.domain.*;
import com.bluetoya.beansontime.refund.application.port.out.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundProcessor {
  private final RefundStore refunds;
  private final RefundGateway gateway;
  private final LoadPaymentPort payments;
  private final LoadBillingPort billings;
  private final SaveBillingPort saveBillings;
  private final BillingExecutionPort execution;

  public void process(PaymentId id) {
    var refund = refunds.find(id).orElseThrow();
    if (refund.isCompleted()) return;
    var payment = payments.load(id).orElseThrow();
    if (payment.getStatus() != PaymentStatus.SUCCESS)
      throw new IllegalStateException("성공 결제만 고객 환불할 수 있습니다.");
    // 영속화된 환불 요청만 트랜잭션 밖에서 전송한다.
    gateway
        .refundOrFind(refund)
        .ifPresent(
            receipt -> {
              var initial = billings.load(payment.getBillingId()).orElseThrow();
              execution.execute(
                  initial.getSubscriptionId(),
                  () -> {
                    var current = refunds.find(id).orElseThrow();
                    if (current.isCompleted()) return null;
                    var billing = billings.load(payment.getBillingId()).orElseThrow();
                    billing.refund();
                    refunds.save(current.complete(receipt.transactionId(), receipt.completedAt()));
                    saveBillings.save(billing);
                    return null;
                  });
            });
  }
}
