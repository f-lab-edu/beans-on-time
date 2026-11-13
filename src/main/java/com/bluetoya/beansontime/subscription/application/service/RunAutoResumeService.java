package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.in.RunAutoResumeUseCase;
import com.bluetoya.beansontime.subscription.application.port.out.FindDueAutoResumeSubscriptionsPort;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SubscriptionExecutionPort;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunAutoResumeService implements RunAutoResumeUseCase {
  private final FindDueAutoResumeSubscriptionsPort candidates;
  private final LoadSubscriptionPort subscriptions;
  private final SaveSubscriptionPort saveSubscriptions;
  private final SubscriptionExecutionPort execution;
  private final Clock clock;

  @Override
  public void runDue() {
    LocalDate businessDate = LocalDate.now(clock);
    SubscriptionId after = null;
    while (true) {
      var page = candidates.findDue(businessDate, after, 100);
      if (page.isEmpty()) return;
      for (var id : page) {
        try {
          execution.execute(
              id,
              () -> {
                var subscription =
                    subscriptions
                        .load(id)
                        .orElseThrow(
                            () -> new SubscriptionNotFoundException("자동 재개할 구독이 존재하지 않습니다."));
                if (subscription.isAutoResumeDue(businessDate)) {
                  subscription.resume(businessDate);
                  saveSubscriptions.save(subscription);
                }
                return null;
              });
        } catch (RuntimeException exception) {
          log.warn("구독 자동 재개 확인 필요: subscriptionId={}", id.value(), exception);
        }
      }
      after = page.getLast();
    }
  }
}
