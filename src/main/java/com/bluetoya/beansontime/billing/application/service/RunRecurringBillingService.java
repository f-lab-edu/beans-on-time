package com.bluetoya.beansontime.billing.application.service;

import com.bluetoya.beansontime.billing.application.port.in.*;
import com.bluetoya.beansontime.subscription.application.port.out.FindDueSubscriptionsPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RunRecurringBillingService implements RunRecurringBillingUseCase {
  private final FindDueSubscriptionsPort candidates;
  private final ProcessRecurringBillingService process;
  private final Clock clock;

  public void runDue() {
    LocalDate date = LocalDate.now(clock);
    SubscriptionId after = null;
    while (true) {
      var page = candidates.findDue(date, after, 100);
      if (page.isEmpty()) return;
      for (var target : page) {
        try {
          process.process(target.subscriptionId(), target.dueDate(), date);
        } catch (RuntimeException exception) {
          log.warn(
              "정기결제 처리 확인 필요: subscriptionId={}, dueDate={}",
              target.subscriptionId().value(),
              target.dueDate(),
              exception);
        }
      }
      // 상태가 바뀌어 대상에서 사라져도 다음 구독을 건너뛰지 않도록 ID 기준으로 진행한다.
      after = page.getLast().subscriptionId();
    }
  }
}
