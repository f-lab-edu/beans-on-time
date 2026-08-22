package com.bluetoya.beansontime.product.application.service;

import com.bluetoya.beansontime.product.application.port.in.RegisterProductCommand;
import com.bluetoya.beansontime.product.application.port.in.RegisterProductUseCase;
import com.bluetoya.beansontime.product.application.port.out.CurrentSellerProvider;
import com.bluetoya.beansontime.product.application.port.out.SaveProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import com.bluetoya.beansontime.product.domain.SellerId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterProductService implements RegisterProductUseCase {

    private final SaveProductPort saveProductPort;
    private final CurrentSellerProvider currentSellerIdProvider;

    @Override
    public ProductId register(RegisterProductCommand command) {
        SellerId sellerId = currentSellerIdProvider.getCurrentSellerId();

        Product product = new Product(sellerId, command.name(), command.basePrice());
        saveProductPort.save(product);
        return product.getId();
    }
}
