package com.bluetoya.beansontime.payment.application.port.out;

import com.bluetoya.beansontime.payment.domain.*;
import java.util.Optional;

public interface PaymentApprovalPort {
  Optional<PaymentApproval> load(PaymentId id);

  void saveNew(PaymentApproval approval);

  void save(PaymentApproval approval);
}
