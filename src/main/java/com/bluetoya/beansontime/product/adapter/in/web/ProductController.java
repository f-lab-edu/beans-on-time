package com.bluetoya.beansontime.product.adapter.in.web;

import com.bluetoya.beansontime.product.adapter.in.web.request.RegisterProductRequest;
import com.bluetoya.beansontime.product.application.port.in.DiscontinueProductUseCase;
import com.bluetoya.beansontime.product.application.port.in.RegisterProductCommand;
import com.bluetoya.beansontime.product.application.port.in.RegisterProductUseCase;
import com.bluetoya.beansontime.product.application.port.in.ResumeProductSupplyUseCase;
import com.bluetoya.beansontime.product.application.port.in.StopProductSupplyUseCase;
import com.bluetoya.beansontime.product.domain.Money;
import com.bluetoya.beansontime.product.domain.ProductId;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

  private final RegisterProductUseCase registerProductUseCase;
  private final StopProductSupplyUseCase stopProductSupplyUseCase;
  private final ResumeProductSupplyUseCase resumeProductSupplyUseCase;
  private final DiscontinueProductUseCase discontinueProductUseCase;

  @PostMapping
  long register(@RequestBody RegisterProductRequest request) {
    ProductId productId = registerProductUseCase.register(toCommand(request));
    return productId.id();
  }

  @PatchMapping("/{id}/supply/stop")
  void stopSupply(@PathVariable @Positive(message = "상품 ID는 0보다 커야 합니다.") long id) {
    stopProductSupplyUseCase.stopSupply(new ProductId(id));
  }

  @PatchMapping("/{id}/supply/resume")
  void resumeSupply(@PathVariable @Positive(message = "상품 ID는 0보다 커야 합니다.") long id) {
    resumeProductSupplyUseCase.resumeSupply(new ProductId(id));
  }

  @PatchMapping("/{id}/discontinue")
  void discontinue(@PathVariable @Positive(message = "상품 ID는 0보다 커야 합니다.") long id) {
    discontinueProductUseCase.discontinue(new ProductId(id));
  }

  private RegisterProductCommand toCommand(RegisterProductRequest request) {
    return new RegisterProductCommand(request.name(), new Money(request.basePrice()));
  }
}
