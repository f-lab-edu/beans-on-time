package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.port.out.ExistsPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.SavePaymentPort;
import com.bluetoya.beansontime.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryPaymentAdapter implements SavePaymentPort, ExistsPaymentPort {
  private final InMemoryPaymentRepository paymentRepository;

  @Override
  public void save(Payment payment) {
    paymentRepository.save(payment);
  }

  @Override
  public boolean existsByBillingId(BillingId billingId) {
    return paymentRepository.existsByBillingId(billingId);
  }
}
