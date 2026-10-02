package com.bluetoya.beansontime.payment.adapter.out.persistence;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.port.out.FindProcessingPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.LoadPaymentPort;
import com.bluetoya.beansontime.payment.application.port.out.SavePaymentPort;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryPaymentAdapter
    implements SavePaymentPort, FindProcessingPaymentPort, LoadPaymentPort {
  private final InMemoryPaymentRepository paymentRepository;

  @Override
  public void save(Payment payment) {
    paymentRepository.save(payment);
  }

  @Override
  public Optional<Payment> load(PaymentId id) {
    return paymentRepository.findById(id);
  }

  @Override
  public Optional<Payment> findProcessing(BillingId id) {
    return paymentRepository.findProcessing(id);
  }

  @Override
  public List<PaymentId> findProcessingIds() {
    return paymentRepository.findProcessingIds();
  }
}
