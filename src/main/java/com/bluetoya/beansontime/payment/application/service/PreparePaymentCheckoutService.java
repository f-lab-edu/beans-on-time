package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.billing.application.exception.*;
import com.bluetoya.beansontime.billing.application.port.in.OwnedBillingLoader;
import com.bluetoya.beansontime.billing.application.port.out.BillingExecutionPort;
import com.bluetoya.beansontime.billing.domain.*;
import com.bluetoya.beansontime.payment.application.exception.PaymentInProgressException;
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
          if (billing.getPurpose() != BillingPurpose.REACTIVATION)
            throw new ReactivationBillingNotAllowedException("정기 청구는 고객 재활성화 결제로 실행할 수 없습니다.");
          if (billing.getStatus() == BillingStatus.PAID)
            throw new BillingAlreadyPaidException("이미 결제가 완료된 청구입니다.");
          if (billing.getStatus() == BillingStatus.CANCELLED)
            throw new ReactivationBillingNotAllowedException("보상 취소가 완료된 청구입니다. 새 청구를 준비해 주세요.");
          var processing = payments.findProcessing(id);
          if (processing.isPresent())
            throw new PaymentInProgressException(processing.get().getId());
          if (billing.getStatus() == BillingStatus.EXPIRED
              || billing.isPaymentWindowClosed(LocalDateTime.now(clock)))
            throw new BillingExpiredException("만료된 청구입니다. 새 청구를 준비해 주세요.");
          return new PaymentCheckoutDetail(checkouts.prepare(id), billing.getAmount().price());
        });
  }
}
