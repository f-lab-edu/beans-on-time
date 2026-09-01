package com.bluetoya.beansontime.product.domain;

import com.bluetoya.beansontime.product.domain.exception.InvalidProductStateChangeException;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
public class Product {
  private final ProductId id;
  private final SellerId sellerId;

  private final String name;
  private final String description;
  private final Money basePrice;

  private final List<ProductImage> images;
  private final List<ProductSizeOption> sizeOptions;
  private final Set<GrindType> grindTypes;

  private ProductStatus status;

  public Product(SellerId sellerId, String name, Money basePrice) {
    this.id = ProductId.generate();
    this.sellerId = sellerId;
    this.name = name;
    this.description = "";
    this.basePrice = basePrice;
    this.images = List.of();
    this.sizeOptions = List.of();
    this.grindTypes = Set.of();
    this.status = ProductStatus.AVAILABLE;
  }

  public void stopSupply() {
    if (status == ProductStatus.DISCONTINUED) {
      throw new InvalidProductStateChangeException("영구 종료된 상품의 공급을 일시 중지할 수 없습니다.");
    }

    status = ProductStatus.TEMPORARILY_UNAVAILABLE;
  }

  public void resumeSupply() {
    if (status == ProductStatus.DISCONTINUED) {
      throw new InvalidProductStateChangeException("영구 종료된 상품의 공급을 재개할 수 없습니다.");
    }

    status = ProductStatus.AVAILABLE;
  }

  public void discontinue() {
    status = ProductStatus.DISCONTINUED;
  }

  public boolean isSubscribable() {
    return status == ProductStatus.AVAILABLE;
  }

  public record ProductImage(String url, int order) {}

  public record ProductSizeOption(int gramSize, Money additionalPrice) {}
}
