package com.bluetoya.beansontime.security.aspect;

import com.bluetoya.beansontime.security.application.CurrentActorProvider;
import com.bluetoya.beansontime.security.authorization.OwnershipResolver;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnershipAspect {

    private final CurrentActorProvider currentActorProvider;
    private final List<OwnershipResolver<?>> ownershipResolvers;

    @AfterReturning(
            pointcut =
                    "@annotation(com.bluetoya.beansontime.security.annotation.RequireOwnership)",
            returning = "resource"
    )
    public void authorize(Object resource) {

        if (resource == null) {
            throw new IllegalStateException(
                    "Ownership authorization requires a returned resource"
            );
        }

        OwnershipResolver<?> resolver =
                ownershipResolvers.stream()
                        .filter(it -> it.supports(resource))
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No OwnershipResolver registered for "
                                                + resource.getClass().getName()
                                )
                        );

        ActorIdentity currentActor =
                currentActorProvider.getCurrentActor();

        ActorIdentity owner =
                resolver.resolve(resource);

        if (!owner.equals(currentActor)) {
            throw new AccessDeniedException(
                    "Resource access denied"
            );
        }
    }
}
