package com.bluetoya.beansontime.security.aspect;

import com.bluetoya.beansontime.security.application.CurrentActorProvider;
import com.bluetoya.beansontime.security.authorization.OwnershipResolver;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnershipAspect {

  private final CurrentActorProvider currentActorProvider;
  private final List<OwnershipResolver<?>> ownershipResolvers;

  @AfterReturning(
      pointcut = "@annotation(com.bluetoya.beansontime.security.annotation.RequireOwnership)",
      returning = "resource")
  public void authorize(Object resource) {

    if (resource == null) {
      throw new IllegalStateException("소유권 인가에는 반환된 리소스가 필요합니다.");
    }

    OwnershipResolver<?> resolver =
        ownershipResolvers.stream()
            .filter(it -> it.supports(resource))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "해당 리소스의 소유권 판별기가 등록되어 있지 않습니다: " + resource.getClass().getName()));

    ActorIdentity currentActor = currentActorProvider.getCurrentActor();

    ActorIdentity owner = resolver.resolve(resource);

    if (!owner.equals(currentActor)) {
      throw new AccessDeniedException("리소스 접근 권한이 없습니다.");
    }
  }
}
