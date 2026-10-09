package com.bluetoya.beansontime.refund.application.port.out;

import com.bluetoya.beansontime.payment.domain.PaymentId;
import com.bluetoya.beansontime.refund.domain.Refund;
import java.util.*;

public interface RefundStore {
  void saveNew(Refund refund);

  void save(Refund refund);

  Optional<Refund> find(PaymentId paymentId);

  List<PaymentId> findPending();

  List<Refund> findBySubscription(com.bluetoya.beansontime.subscription.domain.SubscriptionId id);
}
