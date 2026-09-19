package com.bluetoya.beansontime.product.application.service;

import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.application.port.in.GetProductDetailQuery;
import com.bluetoya.beansontime.product.application.port.in.ProductDetail;
import com.bluetoya.beansontime.product.application.port.out.GetProductDetailQueryPort;
import com.bluetoya.beansontime.product.domain.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProductDetailService implements GetProductDetailQuery {
  private final GetProductDetailQueryPort queryPort;

  public ProductDetail find(ProductId productId) {
    return queryPort
        .find(productId)
        .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));
  }
}
