package com.bluetoya.beansontime.product.adapter.in.web;

import com.bluetoya.beansontime.product.adapter.in.web.request.RegisterProductRequest;
import com.bluetoya.beansontime.product.application.port.in.RegisterProductCommand;
import com.bluetoya.beansontime.product.application.port.in.RegisterProductUseCase;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final RegisterProductUseCase registerProductUseCase;

  @PostMapping
  long register(@RequestBody RegisterProductRequest request) {
    ProductId productId = registerProductUseCase.register(toCommand(request));
    return productId.id();
  }

  private RegisterProductCommand toCommand(RegisterProductRequest request) {
    return new RegisterProductCommand(request.name(), new Money(request.basePrice()));
  }
}
