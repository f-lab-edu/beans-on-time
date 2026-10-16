package com.bluetoya.beansontime.subscription.application.service;

import com.bluetoya.beansontime.billing.application.port.out.FindPendingBillingPort;
import com.bluetoya.beansontime.billing.domain.BillingPurpose;
import com.bluetoya.beansontime.payment.application.port.out.FindProcessingPaymentPort;
import com.bluetoya.beansontime.subscription.application.exception.SubscriptionPauseBlockedByPaymentException;
import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SubscriptionExecutionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PauseSubscriptionService implements PauseSubscriptionUseCase {
  private final OwnedSubscriptionLoader ownedSubscriptionLoader;
  private final SaveSubscriptionPort saveSubscriptionPort;
  private final Clock clock;

  private final SubscriptionExecutionPort subscriptionExecutionPort;
  private final FindPendingBillingPort pendingBillings;
  private final FindProcessingPaymentPort processingPayments;

  @Override
  public void pause(PauseSubscriptionCommand command) {
    subscriptionExecutionPort.execute(
        command.subscriptionId(),
        () -> {
          apply(command);
          return null;
        });
  }

  private void apply(PauseSubscriptionCommand command) {
    Subscription subscription = ownedSubscriptionLoader.load(command.subscriptionId());

    // 정기결제 시작과 같은 실행 잠금 안에서 확인하여 PG 호출 중 상태 변경을 막는다.
    if (pendingBillings
        .findPending(subscription.getId())
        .filter(billing -> billing.getPurpose() == BillingPurpose.RECURRING)
        .flatMap(billing -> processingPayments.findProcessing(billing.getId()))
        .isPresent()) {
      throw new SubscriptionPauseBlockedByPaymentException("정기결제 결과 확인 후 다시 일시정지를 요청해 주세요.");
    }

    subscription.pause(command.pauseUntilDate(), LocalDateTime.now(clock));

    saveSubscriptionPort.save(subscription);
  }
}
