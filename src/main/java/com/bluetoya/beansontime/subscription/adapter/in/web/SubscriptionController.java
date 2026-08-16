package com.bluetoya.beansontime.subscription.adapter.in.web;

import com.bluetoya.beansontime.security.CurrentCustomerId;
import com.bluetoya.beansontime.subscription.adapter.in.web.request.SubscriptionCreateRequest;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.CycleResponse;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscriptionCreateResponse;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscriptionResponse;
import com.bluetoya.beansontime.subscription.application.port.in.*;
import com.bluetoya.beansontime.subscription.domain.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

  private final CreateSubscriptionUseCase createSubscriptionUseCase;
  private final PauseSubscriptionUseCase pauseSubscriptionUseCase;
  private final ResumeSubscriptionUseCase resumeSubscriptionUseCase;
  private final FindSubscriptionQuery findSubscriptionQuery;

  @GetMapping("/{id}")
  SubscriptionResponse find(@PathVariable UUID id, @CurrentCustomerId long customerId) {
    SubscriptionQueryResult result = findSubscriptionQuery.find(new SubscriptionId(id), customerId);
    return toResponse(result);
  }

  @PostMapping
  SubscriptionCreateResponse create(
      @RequestBody SubscriptionCreateRequest request, @CurrentCustomerId long customerId) {
    SubscriptionId subscriptionId =
        createSubscriptionUseCase.subscribe(toCommand(request, customerId));
    return new SubscriptionCreateResponse(subscriptionId.value());
  }

  @PatchMapping("/hold")
  void pause(@RequestParam UUID subscriptionId, @CurrentCustomerId long customerId) {
    pauseSubscriptionUseCase.pause(
        new PauseSubscriptionCommand(
            new SubscriptionId(subscriptionId), new CustomerId(customerId)));
  }

  @PatchMapping("/resume")
  void resume(@RequestParam UUID subscriptionId, @CurrentCustomerId long customerId) {
    resumeSubscriptionUseCase.resume(
        new ResumeSubscriptionCommand(
            new SubscriptionId(subscriptionId), new CustomerId(customerId)));
  }

  private CreateSubscriptionCommand toCommand(SubscriptionCreateRequest request, long customerId) {
    return new CreateSubscriptionCommand(
        new CustomerId(customerId),
        new ProductId(request.productId()),
        new Cycle(CycleUnit.valueOf(request.cycle().unit()), request.cycle().interval()));
  }

  private SubscriptionResponse toResponse(SubscriptionQueryResult result) {
    return new SubscriptionResponse(
        result.subscriptionId(),
        result.customerId(),
        result.productId(),
        new CycleResponse(result.cycleUnit(), result.cycleInterval()),
        result.status());
  }
}
