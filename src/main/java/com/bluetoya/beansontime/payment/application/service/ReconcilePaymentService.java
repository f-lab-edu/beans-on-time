package com.bluetoya.beansontime.payment.application.service;

import com.bluetoya.beansontime.billing.application.port.in.OwnedBillingLoader;
import com.bluetoya.beansontime.payment.application.exception.PaymentNotFoundException;
import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import com.bluetoya.beansontime.payment.application.port.in.ReconcilePaymentCommand;
import com.bluetoya.beansontime.payment.application.port.in.ReconcilePaymentUseCase;
import com.bluetoya.beansontime.payment.application.port.out.LoadPaymentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReconcilePaymentService implements ReconcilePaymentUseCase {
  private final OwnedBillingLoader ownedBillingLoader;
  private final LoadPaymentPort loadPaymentPort;
  private final PaymentResultResolver paymentResultResolver;

  @Override
  public PaymentResult reconcile(ReconcilePaymentCommand command) {
    ownedBillingLoader.load(command.billingId());
    loadPaymentPort
        .load(command.paymentId())
        .filter(payment -> payment.getBillingId().equals(command.billingId()))
        .orElseThrow(() -> new PaymentNotFoundException("청구에 해당하는 결제 시도를 찾을 수 없습니다."));
    return paymentResultResolver.resolve(command.paymentId());
  }
}
