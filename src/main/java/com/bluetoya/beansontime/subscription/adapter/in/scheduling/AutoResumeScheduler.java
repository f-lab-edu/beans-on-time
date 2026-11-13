package com.bluetoya.beansontime.subscription.adapter.in.scheduling;

import com.bluetoya.beansontime.subscription.application.port.in.RunAutoResumeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "subscription.auto-resume.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AutoResumeScheduler {
  private final RunAutoResumeUseCase useCase;

  @Scheduled(
      fixedDelayString = "${subscription.auto-resume.delay-ms:60000}",
      initialDelayString = "${subscription.auto-resume.delay-ms:60000}")
  public void resumeDue() {
    useCase.runDue();
  }
}
