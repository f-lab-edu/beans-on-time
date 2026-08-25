package com.bluetoya.beansontime.subscription.adapter.in.web;

import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PAYMENT_FAILED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.product.application.port.in.ProductAvailability;
import com.bluetoya.beansontime.product.application.port.in.ProductInfo;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscriptionDetailResponse;
import com.bluetoya.beansontime.subscription.application.port.in.GetSubscriptionDetailQuery;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.in.ResumeSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.in.SubscribeUseCase;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionDetail;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionInfo;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionControllerTest {

  @Test
  void mapsExpandedSubscriptionDetailResponse() {
    GetSubscriptionDetailQuery query = mock(GetSubscriptionDetailQuery.class);
    SubscriptionController controller = controller(query, mock(PauseSubscriptionUseCase.class));
    UUID id = UUID.randomUUID();
    SubscriptionInfo subscriptionInfo =
        new SubscriptionInfo(
            id.toString(),
            1,
            "ONE_MONTH",
            1,
            "PAUSED",
            Set.of(PRODUCT_UNAVAILABLE, PAYMENT_FAILED),
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 9, 29),
            31,
            LocalDate.of(2026, 9, 30),
            LocalDateTime.of(2026, 9, 10, 14, 30),
            LocalDate.of(2026, 10, 16),
            true);
    when(query.find(new SubscriptionId(id)))
        .thenReturn(
            new SubscriptionDetail(
                subscriptionInfo,
                new ProductInfo(ProductAvailability.UNAVAILABLE, 10, null, null)));

    SubscriptionDetailResponse response = controller.find(id);

    assertThat(response.subscription().lifecycleStatus()).isEqualTo("PAUSED");
    assertThat(response.subscription().suspensionReasons())
        .containsExactlyInAnyOrder("PRODUCT_UNAVAILABLE", "PAYMENT_FAILED");
    assertThat(response.subscription().startedDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(response.subscription().currentPeriodStartDate())
        .isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(response.subscription().currentPeriodEndDate()).isEqualTo(LocalDate.of(2026, 9, 29));
    assertThat(response.subscription().billingAnchorDay()).isEqualTo(31);
    assertThat(response.subscription().nextBillingDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    assertThat(response.subscription().pausedAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 14, 30));
    assertThat(response.subscription().resumeDate()).isEqualTo(LocalDate.of(2026, 10, 16));
    assertThat(response.subscription().executionBlocked()).isTrue();
    assertThat(response.product().availability()).isEqualTo("UNAVAILABLE");
    assertThat(response.product().basePrice()).isNull();
  }

  @Test
  void mapsClearedPauseContextAfterResume() {
    GetSubscriptionDetailQuery query = mock(GetSubscriptionDetailQuery.class);
    SubscriptionController controller = controller(query, mock(PauseSubscriptionUseCase.class));
    UUID id = UUID.randomUUID();
    SubscriptionInfo subscriptionInfo =
        new SubscriptionInfo(
            id.toString(),
            1,
            "ONE_MONTH",
            1,
            "ACTIVE",
            Set.of(),
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            1,
            LocalDate.of(2026, 10, 1),
            null,
            null,
            false);
    when(query.find(new SubscriptionId(id)))
        .thenReturn(
            new SubscriptionDetail(
                subscriptionInfo,
                new ProductInfo(ProductAvailability.UNAVAILABLE, 10, null, null)));

    SubscriptionDetailResponse response = controller.find(id);

    assertThat(response.subscription().pausedAt()).isNull();
    assertThat(response.subscription().resumeDate()).isNull();
    assertThat(response.subscription().executionBlocked()).isFalse();
  }

  @Test
  void mapsPauseUntilDateToPauseCommand() {
    PauseSubscriptionUseCase pauseUseCase = mock(PauseSubscriptionUseCase.class);
    SubscriptionController controller =
        controller(mock(GetSubscriptionDetailQuery.class), pauseUseCase);
    UUID id = UUID.randomUUID();
    LocalDate pauseUntilDate = LocalDate.of(2026, 9, 15);

    controller.pause(id, pauseUntilDate);

    verify(pauseUseCase)
        .pause(new PauseSubscriptionCommand(new SubscriptionId(id), pauseUntilDate));
  }

  private SubscriptionController controller(
      GetSubscriptionDetailQuery query, PauseSubscriptionUseCase pauseUseCase) {
    return new SubscriptionController(
        mock(SubscribeUseCase.class), pauseUseCase, mock(ResumeSubscriptionUseCase.class), query);
  }
}
