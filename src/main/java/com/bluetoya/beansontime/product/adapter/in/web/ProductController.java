package com.bluetoya.beansontime.product.adapter.in.web;

import com.bluetoya.beansontime.product.adapter.in.web.request.CreateProductRequest;
import com.bluetoya.beansontime.product.application.port.in.CreateProductCommand;
import com.bluetoya.beansontime.product.application.port.in.CreateProductUseCase;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.SellerId;
import com.bluetoya.beansontime.product.domain.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final CreateProductUseCase createProductUseCase;

    public long create(@RequestBody CreateProductRequest request) {
        ProductId productId = createProductUseCase.create(toCommand(request));
        return productId.id();
    }

    private CreateProductCommand toCommand(CreateProductRequest request) {
        return new CreateProductCommand(new SellerId(request.sellerId()), request.name(), new Money(request.basePrice()));
    }
}
