package com.bluetoya.beansontime.product.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.security.application.CurrentActorProvider;
import com.bluetoya.beansontime.security.aspect.OwnershipAspect;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import com.bluetoya.beansontime.seller.domain.SellerId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ProductOwnershipResolverTest {

  @Test
  void resolvesSellerOwnershipFromReturnedProduct() {
    Product product = productOwnedBy(42);

    ActorIdentity owner = new ProductOwnershipResolver().resolveOwner(product);

    assertThat(owner).isEqualTo(new ActorIdentity(ActorType.SELLER, 42));
  }

  @Test
  void ownershipAspectAllowsOwnerAndRejectsAnotherSeller() {
    CurrentActorProvider actorProvider = mock(CurrentActorProvider.class);
    OwnershipAspect aspect =
        new OwnershipAspect(actorProvider, List.of(new ProductOwnershipResolver()));
    Product product = productOwnedBy(42);

    when(actorProvider.getCurrentActor()).thenReturn(new ActorIdentity(ActorType.SELLER, 42));
    assertThatCode(() -> aspect.authorize(product)).doesNotThrowAnyException();

    when(actorProvider.getCurrentActor()).thenReturn(new ActorIdentity(ActorType.SELLER, 99));
    assertThatThrownBy(() -> aspect.authorize(product)).isInstanceOf(AccessDeniedException.class);
  }

  private Product productOwnedBy(long sellerId) {
    return new Product(new SellerId(sellerId), "Ethiopia", new Money(18000));
  }
}
