package com.bluetoya.beansontime.product.application.port.out;

import com.bluetoya.beansontime.seller.domain.SellerId;

public interface CurrentSellerProvider {
  SellerId getCurrentSellerId();
}
