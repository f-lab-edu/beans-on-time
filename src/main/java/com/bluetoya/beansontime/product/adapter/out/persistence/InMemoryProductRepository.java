package com.bluetoya.beansontime.product.adapter.out.persistence;

import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProductRepository {

  private final Map<ProductId, Product> products = new ConcurrentHashMap<>();

  public Optional<Product> findById(ProductId productId) {
    return Optional.ofNullable(products.get(productId));
  }

  void save(Product product) {
    products.put(product.getId(), product);
  }
}
