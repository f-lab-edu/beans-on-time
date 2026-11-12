package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.payment.domain.Payment;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.util.Optional;

public interface FindLatestSuccessfulPaymentPort {
  /** 승인 시각 내림차순, 동률은 ID 내림차순. 승인 시각 미상인 성공 건이 있으면 그 건을 반환하여 자동 판단을 중단한다. */
  Optional<Payment> findLatestSuccess(SubscriptionId id, java.time.LocalDateTime requestedAt);

  java.util.List<Payment> findApprovedAfterRequest(
      SubscriptionId id, java.time.LocalDateTime requestedAt);
}
