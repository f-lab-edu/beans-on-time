package com.bluetoya.beansontime.subscription.adapter.in.web;

import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.subscription.adapter.in.web.request.SubscribeRequest;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscribeResponse;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscriptionDetailResponse;
import com.bluetoya.beansontime.subscription.application.port.in.*;
import com.bluetoya.beansontime.subscription.domain.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
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
  SubscribeResponse create(@RequestBody SubscribeRequest request) {
    SubscriptionId subscriptionId = subscribeUseCase.subscribe(toCommand(request));
    return new SubscribeResponse(subscriptionId.value());
  }

  @PatchMapping("/hold")
  void pause(@RequestParam UUID subscriptionId) {
    pauseSubscriptionUseCase.pause(
        new PauseSubscriptionCommand(new SubscriptionId(subscriptionId)));
  }

  @PatchMapping("/resume")
  void resume(@RequestParam UUID subscriptionId) {
    resumeSubscriptionUseCase.resume(
        new ResumeSubscriptionCommand(new SubscriptionId(subscriptionId)));
  }

  private SubscribeCommand toCommand(SubscribeRequest request) {
    return new SubscribeCommand(
        new ProductId(request.productId()),
        new Cycle(CycleUnit.valueOf(request.cycle().unit()), request.cycle().interval()));
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
        subscription.cycleUnit(),
        subscription.cycleInterval(),
        subscription.status());
  }

  private SubscriptionDetailResponse.ProductResponse toProductResponse(ProductInfo product) {
    return new SubscriptionDetailResponse.ProductResponse(
        product.availability().name(), product.productId(), product.name(), product.basePrice());
  }
}
