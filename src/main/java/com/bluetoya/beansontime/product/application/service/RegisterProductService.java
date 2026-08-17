package com.bluetoya.beansontime.product.application.service;

import com.bluetoya.beansontime.product.application.port.in.RegisterProductCommand;
import com.bluetoya.beansontime.product.application.port.in.RegisterProductUseCase;
import com.bluetoya.beansontime.product.application.port.out.SaveProductPort;
import com.bluetoya.beansontime.product.domain.Product;
import com.bluetoya.beansontime.product.domain.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterProductService implements RegisterProductUseCase {

    private final SaveProductPort saveProductPort;

    @Override
    public ProductId register(RegisterProductCommand command) {
        Product product = new Product(command.sellerId(), command.name(), command.basePrice());
        saveProductPort.save(product);
        return product.getId();
    }
}
