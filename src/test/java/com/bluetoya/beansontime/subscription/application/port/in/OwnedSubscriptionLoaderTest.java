package com.bluetoya.beansontime.subscription.application.port.in;

import com.bluetoya.beansontime.subscription.application.policy.SubscriptionAccessPolicy;
import com.bluetoya.beansontime.subscription.application.port.out.LoadSubscriptionPort;
import com.bluetoya.beansontime.subscription.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import com.bluetoya.beansontime.subscription.fixture.SubscriptionFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class OwnedSubscriptionLoaderTest {
    @Mock
    private LoadSubscriptionPort loadSubscriptionPort;

    @Mock
    private SubscriptionAccessPolicy accessPolicy;

    @InjectMocks
    private OwnedSubscriptionLoader ownedSubscriptionLoader;

    @Test
    void 구독을_조회하고_소유권을_검증한_뒤_반환한다() {
        // given
        SubscriptionId subscriptionId =
                new SubscriptionId(UUID.randomUUID());
        CustomerId customerId = new CustomerId(1L);

        Subscription subscription =
                SubscriptionFixture.createSubscription();

        given(loadSubscriptionPort.load(subscriptionId))
                .willReturn(Optional.of(subscription));

        // when
        Subscription result =
                ownedSubscriptionLoader.loadOwnedSubscription(
                        customerId,
                        subscriptionId
                );

        // then
        assertThat(result).isSameAs(subscription);

        then(accessPolicy)
                .should()
                .validateOwner(subscription, customerId);
    }

    @Test
    void 구독이_존재하지_않으면_예외를_던진다() {
        // given
        SubscriptionId subscriptionId =
                new SubscriptionId(UUID.randomUUID());
        CustomerId customerId = new CustomerId(1L);

        given(loadSubscriptionPort.load(subscriptionId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                ownedSubscriptionLoader.loadOwnedSubscription(
                        customerId,
                        subscriptionId
                )
        ).isInstanceOf(IllegalAccessError.class);

        then(accessPolicy)
                .shouldHaveNoInteractions();
    }
}
