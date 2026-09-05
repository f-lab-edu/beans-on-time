package com.bluetoya.beansontime.product.adapter.in.web;

import com.bluetoya.beansontime.product.adapter.in.web.request.RegisterProductRequest;
import com.bluetoya.beansontime.product.adapter.in.web.response.ProductDetailResponse;
import com.bluetoya.beansontime.product.adapter.in.web.response.RegisterProductResponse;
import com.bluetoya.beansontime.product.application.port.in.GetProductDetailQuery;
import com.bluetoya.beansontime.product.application.port.in.ProductDetail;
import com.bluetoya.beansontime.product.application.port.in.RegisterProductCommand;
import com.bluetoya.beansontime.product.application.port.in.RegisterProductUseCase;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.ProductId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final RegisterProductUseCase registerProductUseCase;
  private final GetProductDetailQuery getProductDetailQuery;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  RegisterProductResponse register(@Valid @RequestBody RegisterProductRequest request) {
    ProductId productId = registerProductUseCase.register(toCommand(request));
    return new RegisterProductResponse(productId.id());
  }

  @GetMapping("/{id}")
  ProductDetailResponse find(@PathVariable @Positive long id) {
    ProductDetail detail = getProductDetailQuery.find(new ProductId(id));
    return new ProductDetailResponse(
        detail.productId(),
        detail.name(),
        detail.description(),
        detail.basePrice(),
        detail.status());
  }

  private RegisterProductCommand toCommand(RegisterProductRequest request) {
    return new RegisterProductCommand(request.name(), new Money(request.basePrice()));
  }
}
