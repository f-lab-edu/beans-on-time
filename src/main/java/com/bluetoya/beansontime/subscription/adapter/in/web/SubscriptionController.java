package com.bluetoya.beansontime.subscription.adapter.in.web;

import com.bluetoya.beansontime.product.application.port.in.ProductInfo;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.adapter.in.web.request.SubscribeRequest;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscribeResponse;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscriptionDetailResponse;
import com.bluetoya.beansontime.subscription.application.port.in.*;
import com.bluetoya.beansontime.subscription.domain.*;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Valid
public class SubscriptionController {

  private final SubscribeUseCase subscribeUseCase;
  private final PauseSubscriptionUseCase pauseSubscriptionUseCase;
  private final ResumeSubscriptionUseCase resumeSubscriptionUseCase;
  private final GetSubscriptionDetailQuery findSubscriptionQuery;

  @GetMapping("/{id}")
  SubscriptionDetailResponse find(@PathVariable UUID id) {
    SubscriptionDetail result = findSubscriptionQuery.find(new SubscriptionId(id));
    return toResponse(result);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  SubscribeResponse create(@RequestBody SubscribeRequest request) {
    SubscriptionId subscriptionId = subscribeUseCase.subscribe(toCommand(request));
    return new SubscribeResponse(subscriptionId.value());
  }

  @PatchMapping("/{id}/pause")
  void pause(@PathVariable UUID id, @RequestParam @NotNull LocalDate pauseUntilDate) {
    pauseSubscriptionUseCase.pause(
        new PauseSubscriptionCommand(new SubscriptionId(id), pauseUntilDate));
  }

  @PatchMapping("/{id}/resume")
  void resume(@PathVariable UUID id) {
    resumeSubscriptionUseCase.resume(new ResumeSubscriptionCommand(new SubscriptionId(id)));
  }

  private SubscribeCommand toCommand(SubscribeRequest request) {
    return new SubscribeCommand(
        new ProductId(request.productId()),
        new DeliveryCycle(
            DeliveryCycleUnit.of(request.deliveryCycle().unit()),
            request.deliveryCycle().interval()));
  }

  private SubscriptionDetailResponse toResponse(SubscriptionDetail detail) {
    return new SubscriptionDetailResponse(
        toSubscriptionResponse(detail.subscriptionInfo()), toProductResponse(detail.productInfo()));
  }

  private SubscriptionDetailResponse.SubscriptionResponse toSubscriptionResponse(
      SubscriptionInfo subscription) {
    return new SubscriptionDetailResponse.SubscriptionResponse(
        subscription.subscriptionId(),
        subscription.customerId(),
        subscription.deliveryCycleUnit(),
        subscription.deliveryCycleInterval(),
        subscription.lifecycleStatus(),
        subscription.suspensionReasons().stream()
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet()),
        subscription.startedDate(),
        subscription.currentPeriodStartDate(),
        subscription.currentPeriodEndDate(),
        subscription.remainingPaidDays(),
        subscription.billingAnchorDay(),
        subscription.nextBillingDate(),
        subscription.pausedAt(),
        subscription.scheduledResumeDate(),
        subscription.executionBlocked());
  }

  private SubscriptionDetailResponse.ProductResponse toProductResponse(ProductInfo product) {
    return new SubscriptionDetailResponse.ProductResponse(
        product.availability().name(), product.productId(), product.name(), product.basePrice());
  }
}
