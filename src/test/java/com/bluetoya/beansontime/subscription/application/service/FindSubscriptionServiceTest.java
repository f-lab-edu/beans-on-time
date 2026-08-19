package com.bluetoya.beansontime.subscription.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.application.port.out.FindSubscriptionQueryPort;
import com.bluetoya.beansontime.subscription.domain.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FindSubscriptionServiceTest {

  @Mock private FindSubscriptionQueryPort findSubscriptionPort;

  @InjectMocks FindSubscriptionService findSubscriptionService;

  @Test
  void 고객이_신청한_구독_내역을_조회한다() {
    // given
    UUID uuid = UUID.randomUUID();
    SubscriptionId subscriptionId = new SubscriptionId(uuid);

    long ownerCustomerId = 1L;

    SubscriptionQueryResult expected =
        new SubscriptionQueryResult(uuid, ownerCustomerId, 1L, "ONE_MONTH", 1, "ACTIVE");

    given(findSubscriptionPort.find(subscriptionId)).willReturn(expected);

    // when
    SubscriptionQueryResult actual = findSubscriptionService.find(subscriptionId);

    // then
    assertThat(actual).isEqualTo(expected);
  }
}
