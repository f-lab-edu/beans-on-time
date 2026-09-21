package com.bluetoya.beansontime.subscription.adapter.in.web;

import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PAYMENT_FAILED;
import static com.bluetoya.beansontime.subscription.domain.SubscriptionSuspensionReason.PRODUCT_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluetoya.beansontime.product.application.port.in.ProductAvailability;
import com.bluetoya.beansontime.product.application.port.in.ProductInfo;
import com.bluetoya.beansontime.subscription.adapter.in.web.request.DeliveryCycleRequest;
import com.bluetoya.beansontime.subscription.adapter.in.web.request.SubscribeRequest;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscriptionDetailResponse;
import com.bluetoya.beansontime.subscription.application.port.in.GetSubscriptionDetailQuery;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.PauseSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.in.ResumeSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.ResumeSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.in.SubscribeCommand;
import com.bluetoya.beansontime.subscription.application.port.in.SubscribeUseCase;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionDetail;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionInfo;
import com.bluetoya.beansontime.subscription.domain.DeliveryCycleUnit;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

class SubscriptionControllerTest {

  @Test
  void mapsPausedSubscriptionDetailWithDeliveryAndPauseTerms() {
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
            null,
            null,
            19,
            31,
            LocalDate.of(2026, 11, 4),
            LocalDateTime.of(2026, 9, 10, 14, 30),
            LocalDate.of(2026, 10, 16),
            true);
    when(query.find(new SubscriptionId(id)))
        .thenReturn(
            new SubscriptionDetail(
                subscriptionInfo,
                new ProductInfo(ProductAvailability.UNAVAILABLE, 10, null, null)));

    SubscriptionDetailResponse response = controller.find(id);

    assertThat(response.subscription().deliveryCycleUnit()).isEqualTo("ONE_MONTH");
    assertThat(response.subscription().deliveryCycleInterval()).isEqualTo(1);
    assertThat(response.subscription().lifecycleStatus()).isEqualTo("PAUSED");
    assertThat(response.subscription().suspensionReasons())
        .containsExactlyInAnyOrder("PRODUCT_UNAVAILABLE", "PAYMENT_FAILED");
    assertThat(response.subscription().currentPeriodStartDate()).isNull();
    assertThat(response.subscription().currentPeriodEndDate()).isNull();
    assertThat(response.subscription().remainingPaidDays()).isEqualTo(19);
    assertThat(response.subscription().nextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 4));
    assertThat(response.subscription().pausedAt()).isEqualTo(LocalDateTime.of(2026, 9, 10, 14, 30));
    assertThat(response.subscription().scheduledResumeDate()).isEqualTo(LocalDate.of(2026, 10, 16));
    assertThat(response.subscription().executionBlocked()).isTrue();
  }

  @Test
  void mapsActiveSubscriptionWithoutPausedStateFields() {
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
            null,
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

    assertThat(response.subscription().currentPeriodStartDate())
        .isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(response.subscription().remainingPaidDays()).isNull();
    assertThat(response.subscription().pausedAt()).isNull();
    assertThat(response.subscription().scheduledResumeDate()).isNull();
    assertThat(response.subscription().executionBlocked()).isFalse();
  }

  @Test
  void mapsDeliveryCycleRequestToSubscribeCommand() {
    SubscribeUseCase subscribeUseCase = mock(SubscribeUseCase.class);
    SubscriptionController controller =
        new SubscriptionController(
            subscribeUseCase,
            mock(PauseSubscriptionUseCase.class),
            mock(ResumeSubscriptionUseCase.class),
            mock(GetSubscriptionDetailQuery.class));
    when(subscribeUseCase.subscribe(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new SubscriptionId(UUID.randomUUID()));

    controller.create(new SubscribeRequest(10, new DeliveryCycleRequest("ONE_MONTH", 1)));

    ArgumentCaptor<SubscribeCommand> captor = ArgumentCaptor.forClass(SubscribeCommand.class);
    verify(subscribeUseCase).subscribe(captor.capture());
    assertThat(captor.getValue().productId().id()).isEqualTo(10);
    assertThat(captor.getValue().deliveryCycle().unit()).isEqualTo(DeliveryCycleUnit.ONE_MONTH);
    assertThat(captor.getValue().deliveryCycle().interval()).isEqualTo(1);
  }

  @Test
  void mapsPathSubscriptionIdAndPauseUntilDateToPauseCommand() {
    PauseSubscriptionUseCase pauseUseCase = mock(PauseSubscriptionUseCase.class);
    SubscriptionController controller =
        controller(mock(GetSubscriptionDetailQuery.class), pauseUseCase);
    UUID id = UUID.randomUUID();
    LocalDate pauseUntilDate = LocalDate.of(2026, 9, 15);

    controller.pause(id, pauseUntilDate);

    verify(pauseUseCase)
        .pause(new PauseSubscriptionCommand(new SubscriptionId(id), pauseUntilDate));
  }

  @Test
  void mapsPathSubscriptionIdToResumeCommand() {
    ResumeSubscriptionUseCase resumeUseCase = mock(ResumeSubscriptionUseCase.class);
    SubscriptionController controller =
        new SubscriptionController(
            mock(SubscribeUseCase.class),
            mock(PauseSubscriptionUseCase.class),
            resumeUseCase,
            mock(GetSubscriptionDetailQuery.class));
    UUID id = UUID.randomUUID();

    controller.resume(id);

    verify(resumeUseCase).resume(new ResumeSubscriptionCommand(new SubscriptionId(id)));
  }

  @Test
  void exposesPauseAndResumeAsPathBasedEndpointsWithoutHoldTerm() throws Exception {
    Method pause =
        SubscriptionController.class.getDeclaredMethod("pause", UUID.class, LocalDate.class);
    Method resume = SubscriptionController.class.getDeclaredMethod("resume", UUID.class);

    assertThat(pause.getAnnotation(PatchMapping.class).value()).containsExactly("/{id}/pause");
    assertThat(resume.getAnnotation(PatchMapping.class).value()).containsExactly("/{id}/resume");
    assertThat(pause.getParameters()[0].isAnnotationPresent(PathVariable.class)).isTrue();
    assertThat(pause.getParameters()[1].isAnnotationPresent(RequestParam.class)).isTrue();
    assertThat(resume.getParameters()[0].isAnnotationPresent(PathVariable.class)).isTrue();
    assertThat(
            Arrays.stream(SubscriptionController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PatchMapping.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.value())))
        .noneMatch(path -> path.contains("hold"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ONE_WEEK", "ONE_MONTH"})
  void returnsCreatedWithSubscriptionId(String unit) throws Exception {
    SubscribeUseCase useCase = mock(SubscribeUseCase.class);
    UUID id = UUID.randomUUID();
    when(useCase.subscribe(org.mockito.ArgumentMatchers.any())).thenReturn(new SubscriptionId(id));
    var controller =
        new SubscriptionController(
            useCase,
            mock(PauseSubscriptionUseCase.class),
            mock(ResumeSubscriptionUseCase.class),
            mock(GetSubscriptionDetailQuery.class));
    var mvc = MockMvcBuilders.standaloneSetup(controller).build();
    mvc.perform(
            post("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"productId\":10,\"deliveryCycle\":{\"unit\":\"%s\",\"interval\":1}}"
                        .formatted(unit)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.subscriptionId").value(id.toString()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"productId\":10}",
        "{\"productId\":10,\"deliveryCycle\":null}",
        "{\"deliveryCycle\":{\"unit\":\"ONE_MONTH\",\"interval\":1}}",
        "{\"productId\":0,\"deliveryCycle\":{\"unit\":\"ONE_MONTH\",\"interval\":1}}",
        "{\"productId\":-1,\"deliveryCycle\":{\"unit\":\"ONE_MONTH\",\"interval\":1}}",
        "{\"productId\":10,\"deliveryCycle\":{\"interval\":1}}",
        "{\"productId\":10,\"deliveryCycle\":{\"unit\":null,\"interval\":1}}",
        "{\"productId\":10,\"deliveryCycle\":{\"unit\":\"UNKNOWN\",\"interval\":1}}",
        "{\"productId\":10,\"deliveryCycle\":{\"unit\":\"1달\",\"interval\":1}}",
        "{\"productId\":10,\"deliveryCycle\":{\"unit\":\"\",\"interval\":1}}",
        "{\"productId\":10,\"deliveryCycle\":{\"unit\":\"ONE_MONTH\"}}",
        "{\"productId\":10,\"deliveryCycle\":{\"unit\":\"ONE_MONTH\",\"interval\":0}}",
        "{\"productId\":10,\"deliveryCycle\":{\"unit\":\"ONE_MONTH\",\"interval\":-1}}"
      })
  void rejectsInvalidRequestsBeforeCallingUseCase(String body) throws Exception {
    SubscribeUseCase useCase = mock(SubscribeUseCase.class);
    var controller =
        new SubscriptionController(
            useCase,
            mock(PauseSubscriptionUseCase.class),
            mock(ResumeSubscriptionUseCase.class),
            mock(GetSubscriptionDetailQuery.class));
    var mvc = MockMvcBuilders.standaloneSetup(controller).build();
    mvc.perform(post("/subscriptions").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(useCase);
  }

  private SubscriptionController controller(
      GetSubscriptionDetailQuery query, PauseSubscriptionUseCase pauseUseCase) {
    return new SubscriptionController(
        mock(SubscribeUseCase.class), pauseUseCase, mock(ResumeSubscriptionUseCase.class), query);
  }
}
