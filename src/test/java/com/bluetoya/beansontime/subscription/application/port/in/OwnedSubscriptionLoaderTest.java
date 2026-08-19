package com.bluetoya.beansontime.subscription.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bluetoya.beansontime.subscription.application.exception.SubscriptionNotFoundException;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import com.bluetoya.beansontime.subscription.fixture.SubscriptionFixture;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OwnedSubscriptionLoaderTest {
  @Mock private LoadSubscriptionPort loadSubscriptionPort;
  @InjectMocks private OwnedSubscriptionLoader ownedSubscriptionLoader;

  @Test
  void 구독을_조회하고_소유권을_검증한_뒤_반환한다() {
    // given
    SubscriptionId subscriptionId = new SubscriptionId(UUID.randomUUID());

    Subscription subscription = SubscriptionFixture.createSubscription();

    given(loadSubscriptionPort.load(subscriptionId)).willReturn(Optional.of(subscription));

    // when
    Subscription result = ownedSubscriptionLoader.load(subscriptionId);

    // then
    assertThat(result).isSameAs(subscription);
  }

  @Test
  void 구독이_존재하지_않으면_예외를_던진다() {
    // given
    SubscriptionId subscriptionId = new SubscriptionId(UUID.randomUUID());

    given(loadSubscriptionPort.load(subscriptionId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
            () -> ownedSubscriptionLoader.load(subscriptionId))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }
}
