package com.bluetoya.beansontime.product.application.port.in;

import com.bluetoya.beansontime.product.domain.ProductId;

public interface StopProductSupplyUseCase {
  void stopSupply(ProductId productId);
}
