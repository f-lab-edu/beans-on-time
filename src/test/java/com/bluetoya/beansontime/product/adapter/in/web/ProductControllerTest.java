package com.bluetoya.beansontime.product.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluetoya.beansontime.product.application.exception.ProductNotFoundException;
import com.bluetoya.beansontime.product.application.port.in.DiscontinueProductUseCase;
import com.bluetoya.beansontime.product.application.port.in.RegisterProductUseCase;
import com.bluetoya.beansontime.product.application.port.in.ResumeProductSupplyUseCase;
import com.bluetoya.beansontime.product.application.port.in.StopProductSupplyUseCase;
import com.bluetoya.beansontime.product.domain.ProductId;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

class ProductControllerTest {

  @Test
  void rejectsZeroAndNegativeProductIdWhenStoppingSupply() throws Exception {
    StopProductSupplyUseCase stopUseCase = mock(StopProductSupplyUseCase.class);
    MockMvc mockMvc =
        mockMvc(
            stopUseCase,
            mock(ResumeProductSupplyUseCase.class),
            mock(DiscontinueProductUseCase.class));

    mockMvc.perform(patch("/products/0/supply/stop")).andExpect(status().isBadRequest());
    mockMvc.perform(patch("/products/-1/supply/stop")).andExpect(status().isBadRequest());

    verify(stopUseCase, never()).stopSupply(any());
  }

  @Test
  void rejectsInvalidProductIdWhenResumingSupply() throws Exception {
    ResumeProductSupplyUseCase resumeUseCase = mock(ResumeProductSupplyUseCase.class);
    MockMvc mockMvc =
        mockMvc(
            mock(StopProductSupplyUseCase.class),
            resumeUseCase,
            mock(DiscontinueProductUseCase.class));

    mockMvc.perform(patch("/products/0/supply/resume")).andExpect(status().isBadRequest());

    verify(resumeUseCase, never()).resumeSupply(any());
  }

  @Test
  void rejectsInvalidProductIdWhenDiscontinuing() throws Exception {
    DiscontinueProductUseCase discontinueUseCase = mock(DiscontinueProductUseCase.class);
    MockMvc mockMvc =
        mockMvc(
            mock(StopProductSupplyUseCase.class),
            mock(ResumeProductSupplyUseCase.class),
            discontinueUseCase);

    mockMvc.perform(patch("/products/-1/discontinue")).andExpect(status().isBadRequest());

    verify(discontinueUseCase, never()).discontinue(any());
  }

  @Test
  void keepsNotFoundPolicyForMissingPositiveProductId() throws Exception {
    StopProductSupplyUseCase stopUseCase = mock(StopProductSupplyUseCase.class);
    ProductId productId = new ProductId(999999);
    doThrow(new ProductNotFoundException("상품을 찾을 수 없습니다.")).when(stopUseCase).stopSupply(productId);
    MockMvc mockMvc =
        mockMvc(
            stopUseCase,
            mock(ResumeProductSupplyUseCase.class),
            mock(DiscontinueProductUseCase.class));

    mockMvc.perform(patch("/products/999999/supply/stop")).andExpect(status().isNotFound());

    verify(stopUseCase).stopSupply(productId);
  }

  @Test
  void mapsProductIdToSupplyAndDiscontinueUseCases() {
    StopProductSupplyUseCase stopUseCase = mock(StopProductSupplyUseCase.class);
    ResumeProductSupplyUseCase resumeUseCase = mock(ResumeProductSupplyUseCase.class);
    DiscontinueProductUseCase discontinueUseCase = mock(DiscontinueProductUseCase.class);
    ProductController controller =
        new ProductController(
            mock(RegisterProductUseCase.class), stopUseCase, resumeUseCase, discontinueUseCase);

    controller.stopSupply(10);
    controller.resumeSupply(10);
    controller.discontinue(10);

    ProductId productId = new ProductId(10);
    verify(stopUseCase).stopSupply(productId);
    verify(resumeUseCase).resumeSupply(productId);
    verify(discontinueUseCase).discontinue(productId);
  }

  @Test
  void exposesBehaviorOrientedPathBasedEndpoints() throws Exception {
    Method stop = ProductController.class.getDeclaredMethod("stopSupply", long.class);
    Method resume = ProductController.class.getDeclaredMethod("resumeSupply", long.class);
    Method discontinue = ProductController.class.getDeclaredMethod("discontinue", long.class);

    assertThat(stop.getAnnotation(PatchMapping.class).value()).containsExactly("/{id}/supply/stop");
    assertThat(resume.getAnnotation(PatchMapping.class).value())
        .containsExactly("/{id}/supply/resume");
    assertThat(discontinue.getAnnotation(PatchMapping.class).value())
        .containsExactly("/{id}/discontinue");
    assertThat(stop.getParameters()[0].isAnnotationPresent(PathVariable.class)).isTrue();
    assertThat(resume.getParameters()[0].isAnnotationPresent(PathVariable.class)).isTrue();
    assertThat(discontinue.getParameters()[0].isAnnotationPresent(PathVariable.class)).isTrue();
  }

  private MockMvc mockMvc(
      StopProductSupplyUseCase stopUseCase,
      ResumeProductSupplyUseCase resumeUseCase,
      DiscontinueProductUseCase discontinueUseCase) {
    ProductController controller =
        new ProductController(
            mock(RegisterProductUseCase.class), stopUseCase, resumeUseCase, discontinueUseCase);
    return MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new ProductExceptionHandler())
        .build();
  }
}
