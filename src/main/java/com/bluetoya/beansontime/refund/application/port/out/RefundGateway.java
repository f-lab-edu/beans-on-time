package com.bluetoya.beansontime.refund.application.port.out;

import com.bluetoya.beansontime.refund.domain.Refund;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RefundGateway {
  Optional<Receipt> refundOrFind(Refund refund);

  record Receipt(String transactionId, LocalDateTime completedAt) {}
}
