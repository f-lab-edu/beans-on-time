package com.bluetoya.beansontime.product.adapter.out.persistence;

import com.bluetoya.beansontime.product.application.port.out.SaveProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryProductAdapter implements SaveProductPort {
  private final InMemoryProductRepository productRepository;

  @Override
  public void save(Product product) {
    productRepository.save(product);
  }
}
