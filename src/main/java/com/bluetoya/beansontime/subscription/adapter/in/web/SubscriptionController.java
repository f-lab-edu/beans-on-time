package com.bluetoya.beansontime.subscription.adapter.in.web;

import com.bluetoya.beansontime.subscription.adapter.in.web.request.SubscriptionCreateRequest;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.CycleResponse;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscriptionCreateResponse;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscriptionResponse;
import com.bluetoya.beansontime.subscription.application.port.in.*;
import com.bluetoya.beansontime.subscription.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final PauseSubscriptionUseCase pauseSubscriptionUseCase;
    private final ResumeSubscriptionUseCase resumeSubscriptionUseCase;
    private final FindSubscriptionQuery findSubscriptionQuery;

    @GetMapping("/{id}")
    SubscriptionResponse find(@PathVariable UUID id) {
        SubscriptionQueryResult result = findSubscriptionQuery.find(new SubscriptionId(id));
        return toResponse(result);
    }

    @PostMapping
    SubscriptionCreateResponse create(@RequestBody SubscriptionCreateRequest request) {
        SubscriptionId subscriptionId = createSubscriptionUseCase.subscribe(toCommand(request));
        return new SubscriptionCreateResponse(subscriptionId.value());
    }

    @PatchMapping("/hold")
    void pause(@RequestParam UUID subscriptionId, @RequestParam long customerId) {
        pauseSubscriptionUseCase.pause(
                new PauseSubscriptionCommand(
                        new SubscriptionId(subscriptionId),
                        new CustomerId(customerId)));
    }

    @PatchMapping("/resume")
    void resume(@RequestParam UUID subscriptionId, @RequestParam long customerId) {
        resumeSubscriptionUseCase.resume(
                new ResumeSubscriptionCommand(
                        new SubscriptionId(subscriptionId),
                        new CustomerId(customerId)
                )
        );
    }

    private CreateSubscriptionCommand toCommand(
            SubscriptionCreateRequest request
    ) {
        return new CreateSubscriptionCommand(
                new CustomerId(request.customerId()),
                new ProductId(request.productId()),
                new Cycle(
                        CycleUnit.valueOf(request.cycle().unit()),
                        request.cycle().interval()
                )
        );
    }

    private SubscriptionResponse toResponse(SubscriptionQueryResult result) {
        return new SubscriptionResponse(
                result.subscriptionId(),
                result.customerId(),
                result.productId(),
                new CycleResponse(
                        result.cycleUnit(),
                        result.cycleInterval()
                ),
                result.status());
    }
}
