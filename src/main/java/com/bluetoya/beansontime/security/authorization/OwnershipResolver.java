package com.bluetoya.beansontime.security.authorization;

import com.bluetoya.beansontime.security.model.ActorIdentity;

public interface OwnershipResolver<T> {

    Class<T> targetType();

    ActorIdentity resolveOwner(T resource);

    default boolean supports(Object resource) {
        return targetType().isInstance(resource);
    }

    default ActorIdentity resolve(Object resource) {
        return resolveOwner(
                targetType().cast(resource)
        );
    }
}