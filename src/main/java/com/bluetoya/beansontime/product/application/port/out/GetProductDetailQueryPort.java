package com.bluetoya.beansontime.product.application.port.out;

import com.bluetoya.beansontime.product.application.port.in.ProductDetail;
import com.bluetoya.beansontime.product.domain.ProductId;
import java.util.Optional;

public interface GetProductDetailQueryPort {
  Optional<ProductDetail> find(ProductId productId);
}
