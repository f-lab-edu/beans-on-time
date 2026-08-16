package com.bluetoya.beansontime.subscription.application.policy;

import com.bluetoya.beansontime.subscription.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.Subscription;
import com.bluetoya.beansontime.subscription.fixture.SubscriptionFixture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SubscriptionAccessPolicyTest {
    private final SubscriptionAccessPolicy accessPolicy =
            new SubscriptionAccessPolicy();

    @Test
    void 구독_소유자는_접근할_수_있다() {
        // given
        CustomerId ownerCustomerId = new CustomerId(1L);
        Subscription subscription =
                SubscriptionFixture.createSubscription();

        // when & then
        assertThatCode(() ->
                accessPolicy.validateOwner(
                        subscription,
                        ownerCustomerId
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void 구독_소유자가_아니면_접근할_수_없다() {
        // given
        CustomerId otherCustomerId = new CustomerId(2L);

        Subscription subscription =
                SubscriptionFixture.createSubscription();

        // when & then
        assertThatThrownBy(() ->
                accessPolicy.validateOwner(
                        subscription,
                        otherCustomerId
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
