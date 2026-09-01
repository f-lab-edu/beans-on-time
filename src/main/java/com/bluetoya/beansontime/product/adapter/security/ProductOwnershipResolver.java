package com.bluetoya.beansontime.product.adapter.security;

import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.security.authorization.OwnershipResolver;
import com.bluetoya.beansontime.security.model.ActorIdentity;
import com.bluetoya.beansontime.security.model.ActorType;
import org.springframework.stereotype.Component;

@Component
public class ProductOwnershipResolver implements OwnershipResolver<Product> {

  @Override
  public Class<Product> targetType() {
    return Product.class;
  }

  @Override
  public ActorIdentity resolveOwner(Product product) {
    return new ActorIdentity(ActorType.SELLER, product.getSellerId().id());
  }
}
