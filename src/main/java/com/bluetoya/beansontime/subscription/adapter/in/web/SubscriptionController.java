package com.bluetoya.beansontime.subscription.adapter.in.web;

import com.bluetoya.beansontime.subscription.adapter.in.web.request.SubscriptionCreateRequest;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.CycleResponse;
import com.bluetoya.beansontime.subscription.adapter.in.web.response.SubscriptionResponse;
import com.bluetoya.beansontime.subscription.application.port.in.CreateSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.CreateSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.application.port.in.GetSubscriptionQuery;
import com.bluetoya.beansontime.subscription.application.port.in.SubscriptionQueryResult;
import com.bluetoya.beansontime.subscription.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final GetSubscriptionQuery subscriptionQuery;

    @GetMapping("/{id}")
    SubscriptionResponse get(@PathVariable UUID id) {
        SubscriptionQueryResult result = subscriptionQuery.get(new SubscriptionId(id));
        return toResponse(result);
    }

    @PostMapping
    void create(@RequestBody SubscriptionCreateRequest request) {
        createSubscriptionUseCase.subscribe(toCommand(request));
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
