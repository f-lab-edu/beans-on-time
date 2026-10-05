package com.bluetoya.beansontime.product.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.application.port.out.SaveProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("in-memory")
@Component
@RequiredArgsConstructor
public class InMemoryProductAdapter implements SaveProductPort, LoadProductPort {
  private final InMemoryProductRepository productRepository;

  @Override
  public void saveNew(Product product) {
    save(product);
  }

  @Override
  public void save(Product product) {
    productRepository.save(product);
  }

  @Override
  public Optional<Product> load(ProductId productId) {
    return productRepository.findById(productId);
  }
}
