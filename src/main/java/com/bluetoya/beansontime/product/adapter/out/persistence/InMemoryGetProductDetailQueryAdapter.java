package com.bluetoya.beansontime.product.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.port.in.ProductDetail;
import com.bluetoya.beansontime.product.application.port.out.GetProductDetailQueryPort;
import com.bluetoya.beansontime.product.domain.ProductId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryGetProductDetailQueryAdapter implements GetProductDetailQueryPort {
  private final InMemoryProductRepository repository;

  public Optional<ProductDetail> find(ProductId productId) {
    return repository
        .findById(productId)
        .map(
            p ->
                new ProductDetail(
                    p.getId().id(),
                    p.getName(),
                    p.getDescription(),
                    p.getBasePrice().price(),
                    p.getSupplyStatus().name()));
  }
}
