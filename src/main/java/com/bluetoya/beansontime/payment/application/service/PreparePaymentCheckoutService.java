package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.billing.application.exception.ReactivationBillingNotAllowedException;
import com.bluetoya.beansontime.billing.application.port.in.OwnedBillingLoader;
import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.billing.domain.*;
import com.bluetoya.beansontime.payment.application.port.in.*;
import com.bluetoya.beansontime.payment.application.port.out.*;
import java.time.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PreparePaymentCheckoutService implements PreparePaymentCheckoutUseCase {
  private final OwnedBillingLoader billings;
  private final BillingExecutionPort execution;
  private final PaymentCheckoutPort checkouts;
  private final FindProcessingPaymentPort payments;
  private final Clock clock;

  public PaymentCheckoutDetail prepare(BillingId id) {
    var initial = billings.load(id);
    return execution.execute(
        initial.getSubscriptionId(),
        () -> {
          var billing = billings.load(id);
          if (billing.getStatus() != BillingStatus.PENDING
              || billing.isPaymentWindowClosed(LocalDateTime.now(clock))
              || payments.findProcessing(id).isPresent())
            throw new ReactivationBillingNotAllowedException("현재 청구로 결제창을 열 수 없습니다.");
          return new PaymentCheckoutDetail(checkouts.prepare(id), billing.getAmount().price());
        });
  }
}
