package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.payment.domain.*;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CancelGatewayPaymentPort {
  Optional<CancellationReceipt> cancelOrFind(Payment payment, PaymentApproval approval);

  record CancellationReceipt(String transactionId, LocalDateTime cancelledAt) {}
}
