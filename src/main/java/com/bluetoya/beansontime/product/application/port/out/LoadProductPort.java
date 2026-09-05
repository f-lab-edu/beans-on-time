package com.bluetoya.beansontime.product.application.port.out;

import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import java.util.Optional;

public interface LoadProductPort {
  Optional<Product> load(ProductId productId);
}
