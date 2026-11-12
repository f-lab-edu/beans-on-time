package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.subscription.application.exception.WithdrawalNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.in.*;
import com.bluetoya.beansontime.subscription.application.port.out.WithdrawalStore;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetWithdrawalService implements GetWithdrawalQuery {
  private final OwnedSubscriptionLoader owned;
  private final WithdrawalStore withdrawals;
  private final WithdrawalResultReader results;

  public WithdrawalResult find(SubscriptionId id) {
    owned.load(id);
    return results.read(
        withdrawals
            .find(id)
            .orElseThrow(() -> new WithdrawalNotFoundException("구독 철회 내역이 존재하지 않습니다.")));
  }
}
