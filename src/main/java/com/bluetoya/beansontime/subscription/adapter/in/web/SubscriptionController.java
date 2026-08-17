package com.bluetoya.beansontime.subscription.adapter.in.web;

import com.bluetoya.beansontime.subscription.adapter.in.web.request.SubscribeRequest;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.CycleResponse;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscribeResponse;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.FindSubscriptionResponse;
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
  private final FindSubscriptionQuery findSubscriptionQuery;

  @GetMapping("/{id}")
  FindSubscriptionResponse find(@PathVariable UUID id) {
    SubscriptionQueryResult result = findSubscriptionQuery.find(new SubscriptionId(id));
    return toResponse(result);
  }

  @PostMapping
  SubscribeResponse create(
      @RequestBody SubscribeRequest request) {
    SubscriptionId subscriptionId =
        subscribeUseCase.subscribe(toCommand(request));
    return new SubscribeResponse(subscriptionId.value());
  }

  @PatchMapping("/hold")
  void pause(@RequestParam UUID subscriptionId) {
    pauseSubscriptionUseCase.pause(
        new PauseSubscriptionCommand(
            new SubscriptionId(subscriptionId)));
  }

  @PatchMapping("/resume")
  void resume(@RequestParam UUID subscriptionId) {
    resumeSubscriptionUseCase.resume(
        new ResumeSubscriptionCommand(
            new SubscriptionId(subscriptionId)));
  }

  private SubscribeCommand toCommand(SubscribeRequest request) {
    return new SubscribeCommand(
        new ProductId(request.productId()),
        new Cycle(CycleUnit.valueOf(request.cycle().unit()), request.cycle().interval()));
  }

  private FindSubscriptionResponse toResponse(SubscriptionQueryResult result) {
    return new FindSubscriptionResponse(
        result.subscriptionId(),
        result.customerId(),
        result.productId(),
        new CycleResponse(result.cycleUnit(), result.cycleInterval()),
        result.status());
  }
}
