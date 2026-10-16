package com.bluetoya.beansontime.product.domain;

import com.bluetoya.beansontime.product.domain.exception.InvalidSupplyStateChangeException;
import com.bluetoya.beansontime.seller.domain.SellerId;
import java.util.List;
import java.util.Objects;
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

  private SupplyStatus supplyStatus;

  public Product(SellerId sellerId, String name, Money basePrice) {
    this(ProductId.generate(), sellerId, name, "", basePrice, SupplyStatus.AVAILABLE);
  }

  private Product(
      ProductId id,
      SellerId sellerId,
      String name,
      String description,
      Money basePrice,
      SupplyStatus supplyStatus) {
    this.id = Objects.requireNonNull(id, "상품 ID는 필수입니다.");
    this.sellerId = Objects.requireNonNull(sellerId, "판매자 ID는 필수입니다.");
    this.name = Objects.requireNonNull(name, "상품명은 필수입니다.");
    this.description = Objects.requireNonNull(description, "상품 설명은 필수입니다.");
    this.basePrice = Objects.requireNonNull(basePrice, "상품 가격은 필수입니다.");
    this.images = List.of();
    this.sizeOptions = List.of();
    this.grindTypes = Set.of();
    this.supplyStatus = Objects.requireNonNull(supplyStatus, "공급 상태는 필수입니다.");
  }

  public static Product restore(
      ProductId id,
      SellerId sellerId,
      String name,
      String description,
      Money basePrice,
      SupplyStatus supplyStatus) {
    return new Product(id, sellerId, name, description, basePrice, supplyStatus);
  }

  public void stopSupply() {
    if (supplyStatus == SupplyStatus.DISCONTINUED) {
      throw new InvalidSupplyStateChangeException("영구 종료된 상품의 공급을 일시 중지할 수 없습니다.");
    }

    supplyStatus = SupplyStatus.TEMPORARILY_UNAVAILABLE;
  }

  public void resumeSupply() {
    if (supplyStatus == SupplyStatus.DISCONTINUED) {
      throw new InvalidSupplyStateChangeException("영구 종료된 상품의 공급을 재개할 수 없습니다.");
    }

    supplyStatus = SupplyStatus.AVAILABLE;
  }

  public void discontinue() {
    supplyStatus = SupplyStatus.DISCONTINUED;
  }

  public boolean isSubscribable() {
    return supplyStatus == SupplyStatus.AVAILABLE;
  }

  public record ProductImage(String url, int order) {}

  public record ProductSizeOption(int gramSize, Money additionalPrice) {}
}
