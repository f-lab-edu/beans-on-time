package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.exception.PaymentNotFoundException;
import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import com.bluetoya.beansontime.payment.application.port.out.FindGatewayPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.LoadPaymentPort;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.payment.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentResultResolver {
  private final LoadPaymentPort loadPaymentPort;
  private final FindGatewayPaymentPort findGatewayPaymentPort;
  private final PaymentCompletion paymentCompletionService;

  public PaymentResult resolve(PaymentId paymentId) {
    var payment =
        loadPaymentPort
            .load(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("결제 시도를 찾을 수 없습니다."));
    if (payment.getStatus() == PaymentStatus.PROCESSING
        || payment.getStatus() == PaymentStatus.CANCEL_PENDING) {
      try {
        if (!paymentCompletionService.recover(paymentId))
          findGatewayPaymentPort
              .find(paymentId)
              .ifPresent(result -> paymentCompletionService.complete(paymentId, result));
      } catch (PaymentGatewayUnavailableException exception) {
        // 조회 장애도 승인 거절의 근거가 아니다. 이후 같은 시도를 다시 조회한다.
      }
    }
    return PayBillingService.toResult(loadPaymentPort.load(paymentId).orElseThrow());
  }
}
