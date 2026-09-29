package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.payment.domain.PaymentId;
import java.util.List;
import java.util.Optional;

public interface FindProcessingPaymentPort {
  Optional<Payment> findProcessing(BillingId billingId);

  List<PaymentId> findProcessingIds();
}
