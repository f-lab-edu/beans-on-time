package com.bluetoya.beansontime.product.adapter.out.security;

import com.bluetoya.beansontime.product.application.port.out.CurrentSellerProvider;
import com.bluetoya.beansontime.product.domain.SellerId;
import com.bluetoya.beansontime.security.application.CurrentActorProvider;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentSellerSecurityAdapter implements CurrentSellerProvider {
    private final CurrentActorProvider currentActorProvider;

    @Override
    public SellerId getCurrentSellerId() {
        ActorIdentity actor = currentActorProvider.getCurrentActor();

        if (actor.type() != ActorType.SELLER) {
            throw new AccessDeniedException("해당 사용자는 판매자가 아닙니다.");
        }

        return new SellerId(actor.id());
    }
}
