package com.bluetoya.beansontime.product.application.port.in;

import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.application.port.out.LoadProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.security.annotation.RequireOwnership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnedProductLoader {
  private final LoadProductPort loadProductPort;

  @RequireOwnership
  public Product load(ProductId productId) {
    return loadProductPort
        .load(productId)
        .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
  }
}
