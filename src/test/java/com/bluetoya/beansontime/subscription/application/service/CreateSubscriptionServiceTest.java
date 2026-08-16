package com.bluetoya.beansontime.subscription.application.service;

import static com.bluetoya.beansontime.subscription.fixture.SubscriptionFixture.createSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bluetoya.beansontime.subscription.application.port.in.CreateSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.out.ExistsSubscriptionPort;
import com.bluetoya.beansontime.subscription.application.port.out.SaveSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateSubscriptionServiceTest {
  @Mock SaveSubscriptionPort saveSubscriptionPort;
  @Mock ExistsSubscriptionPort existsSubscriptionPort;
  @InjectMocks CreateSubscriptionService createSubscriptionService;
  @Captor private ArgumentCaptor<Subscription> subscriptionCaptor;

  @Test
  void 구독을_생성한다() {
    // given
    CreateSubscriptionCommand command = createCommand();
    given(existsSubscriptionPort.isExists(command.customerId(), command.productId()))
        .willReturn(false);
    Subscription subscription = createSubscription();

    // when
    createSubscriptionService.subscribe(command);

    // then
    then(saveSubscriptionPort).should().save(subscriptionCaptor.capture());

    Subscription savedSubscription = subscriptionCaptor.getValue();

    assertThat(savedSubscription.getSubscriptionId()).isNotNull();

    assertThat(savedSubscription.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void 이미_동일한_구독이_존재하면_생성할_수_없다() {
    // given
    CreateSubscriptionCommand command = createCommand();

    given(existsSubscriptionPort.isExists(command.customerId(), command.productId()))
        .willReturn(true);

    // when & then
    assertThatThrownBy(() -> createSubscriptionService.subscribe(command))
        .isInstanceOf(IllegalArgumentException.class);

    then(saveSubscriptionPort).shouldHaveNoInteractions();
  }

  private CreateSubscriptionCommand createCommand() {
    return new CreateSubscriptionCommand(
        new CustomerId(1L), new ProductId(1L), new Cycle(CycleUnit.ONE_MONTH, 1));
  }
}
