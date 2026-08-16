package com.bluetoya.beansontime.subscription.application.service;

import static com.bluetoya.beansontime.subscription.fixture.SubscriptionFixture.createSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import com.bluetoya.beansontime.subscription.application.port.in.OwnedSubscriptionLoader;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PauseSubscriptionServiceTest {
  @Mock OwnedSubscriptionLoader ownedSubscriptionLoader;

  @Mock SaveSubscriptionPort saveSubscriptionPort;

  @InjectMocks PauseSubscriptionService pauseSubscriptionService;

  @Test
  void 일시정지_시킨_구독을_저장한다() {
    // given
    PauseSubscriptionCommand command =
        new PauseSubscriptionCommand(new SubscriptionId(UUID.randomUUID()), new CustomerId(1L));
    Subscription subscription = createSubscription();

    given(
            ownedSubscriptionLoader.loadOwnedSubscription(
                command.customerId(), command.subscriptionId()))
        .willReturn(subscription);

    // when
    pauseSubscriptionService.pause(command);

    // then
    assertThat(subscription.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.PAUSED);

    then(saveSubscriptionPort).should().save(subscription);
  }
}
