package com.bluetoya.beansontime.subscription.application.port.out;

import com.bluetoya.beansontime.subscription.domain.*;
import java.util.Optional;

public interface WithdrawalStore {
  Optional<Withdrawal> find(SubscriptionId id);

  void saveNew(Withdrawal withdrawal);
}
