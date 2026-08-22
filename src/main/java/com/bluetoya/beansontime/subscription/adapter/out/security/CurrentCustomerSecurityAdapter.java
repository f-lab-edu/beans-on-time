package com.bluetoya.beansontime.subscription.adapter.out.security;

import com.bluetoya.beansontime.customer.domain.CustomerId;
import com.bluetoya.beansontime.security.application.CurrentActorProvider;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import com.bluetoya.beansontime.subscription.application.port.out.CurrentCustomerProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentCustomerSecurityAdapter implements CurrentCustomerProvider {
    private final CurrentActorProvider currentActorProvider;

    @Override
    public CustomerId getCurrentCustomerId() {
        ActorIdentity actor =
                currentActorProvider.getCurrentActor();

        if (actor.type() != ActorType.CUSTOMER) {
            throw new AccessDeniedException(
                    "해당 사용자는 고객이 아닙니다."
            );
        }

        return new CustomerId(actor.id());
    }
}
