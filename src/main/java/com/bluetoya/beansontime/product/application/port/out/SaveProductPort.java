package com.bluetoya.beansontime.product.application.port.out;

import com.bluetoya.beansontime.product.domain.Product;

public interface SaveProductPort {
  void save(Product product);
}
