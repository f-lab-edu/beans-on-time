package com.bluetoya.beansontime.subscription.adapter.in.web;

import com.bluetoya.beansontime.subscription.adapter.in.web.request.SubscriptionCreateRequest;
import com.bluetoya.beansontime.subscription.application.port.in.CreateSubscriptionCommand;
import com.bluetoya.beansontime.subscription.application.port.in.CreateSubscriptionUseCase;
import com.bluetoya.beansontime.subscription.domain.CustomerId;
import com.bluetoya.beansontime.subscription.domain.Cycle;
import com.bluetoya.beansontime.subscription.domain.CycleUnit;
import com.bluetoya.beansontime.subscription.domain.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final CreateSubscriptionUseCase createSubscriptionUseCase;

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
}
