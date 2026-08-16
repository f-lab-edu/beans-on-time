package com.bluetoya.beansontime.subscription.application.service;

import static com.bluetoya.beansontime.subscription.fixture.SubscriptionFixture.createPausedSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.application.port.in.ResumeSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResumeSubscriptionServiceTest {
  @Mock SaveSubscriptionPort saveSubscriptionPort;

  @Mock OwnedSubscriptionLoader ownedSubscriptionLoader;

  @InjectMocks ResumeSubscriptionService resumeSubscriptionService;

  @Test
  void 재개된_구독을_저장한다() {
    // given
    ResumeSubscriptionCommand command =
        new ResumeSubscriptionCommand(new SubscriptionId(UUID.randomUUID()), new CustomerId(1L));
    Subscription subscription = createPausedSubscription();

    given(
            ownedSubscriptionLoader.loadOwnedSubscription(
                command.customerId(), command.subscriptionId()))
        .willReturn(subscription);

    // when
    resumeSubscriptionService.resume(command);

    // then
    assertThat(subscription.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

    then(saveSubscriptionPort).should().save(subscription);
  }
}
