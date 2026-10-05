package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.payment.application.exception.PaymentGatewayUnavailableException;
import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import com.bluetoya.beansontime.payment.application.port.out.*;
import com.bluetoya.beansontime.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 커밋된 시도를 외부에 요청하고 기존 결과 반영·복구 경계로 연결한다. */
@Service
@RequiredArgsConstructor
public class PaymentAttemptSubmission {
  private final PaymentGateway gateway;
  private final PaymentCompletion completion;
  private final LoadPaymentPort payments;

  public PaymentResult submit(Payment payment) {
    try {
      var result =
          gateway.pay(
              new PaymentGatewayRequest(
                  payment.getId(), payment.getBillingId(), payment.getAmount()));
      completion.complete(payment.getId(), result);
    } catch (PaymentGatewayUnavailableException exception) {
      // 승인 여부를 모르면 기존 시도의 결과 확인에 맡긴다.
    }
    return PayBillingService.toResult(payments.load(payment.getId()).orElseThrow());
  }
}
