package com.bluetoya.beansontime.billing.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluetoya.beansontime.billing.adapter.in.web.response.BillingCheckoutResponse;
import com.bluetoya.beansontime.billing.adapter.in.web.response.PreparedBillingResponse;
import com.bluetoya.beansontime.billing.application.port.in.BillingCheckoutDetail;
import com.bluetoya.beansontime.billing.application.port.in.GetBillingCheckoutQuery;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingCommand;
import com.bluetoya.beansontime.billing.application.port.in.PrepareReactivationBillingUseCase;
import com.bluetoya.beansontime.billing.application.port.in.PreparedBillingDetail;
import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.subscription.domain.SubscriptionId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BillingControllerTest {

  @Test
  void mapsTheSubscriptionPathToBillingPreparation() {
    PrepareReactivationBillingUseCase prepareUseCase =
        mock(PrepareReactivationBillingUseCase.class);
    UUID subscriptionId = UUID.randomUUID();
    when(prepareUseCase.prepare(
            new PrepareReactivationBillingCommand(new SubscriptionId(subscriptionId))))
        .thenReturn(
            new PreparedBillingDetail(
                11,
                30000,
                LocalDate.of(2026, 9, 2),
                "PENDING",
                LocalDateTime.of(2026, 9, 2, 10, 0)));
    BillingController controller =
        new BillingController(prepareUseCase, mock(GetBillingCheckoutQuery.class));

    PreparedBillingResponse response = controller.prepare(subscriptionId);

    assertThat(response.billingId()).isEqualTo(11);
    assertThat(response.amount()).isEqualTo(30000);
    verify(prepareUseCase)
        .prepare(new PrepareReactivationBillingCommand(new SubscriptionId(subscriptionId)));
  }

  @Test
  void mapsCheckoutWithoutReadingTheCurrentProductPrice() {
    GetBillingCheckoutQuery query = mock(GetBillingCheckoutQuery.class);
    when(query.find(new BillingId(11)))
        .thenReturn(
            new BillingCheckoutDetail(
                1,
                new BillingCheckoutDetail.SubscriptionInfo("subscription-id", "PAUSED"),
                new BillingCheckoutDetail.ProductInfo(10, "Ethiopia"),
                new BillingCheckoutDetail.BillingInfo(
                    11, 30000, LocalDate.of(2026, 9, 2), "PENDING")));
    BillingController controller =
        new BillingController(mock(PrepareReactivationBillingUseCase.class), query);

    BillingCheckoutResponse response = controller.checkout(11);

    assertThat(response.billing().amount()).isEqualTo(30000);
    assertThat(response.product().name()).isEqualTo("Ethiopia");
    verify(query).find(new BillingId(11));
  }

  @Test
  void rejectsZeroAndNegativeBillingIdsAsBadRequests() throws Exception {
    BillingController controller =
        new BillingController(
            mock(PrepareReactivationBillingUseCase.class), mock(GetBillingCheckoutQuery.class));
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    mockMvc.perform(get("/billings/0/checkout")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/billings/-1/checkout")).andExpect(status().isBadRequest());
  }

  @Test
  void rejectsNonUuidSubscriptionIdsAsBadRequests() throws Exception {
    BillingController controller =
        new BillingController(
            mock(PrepareReactivationBillingUseCase.class), mock(GetBillingCheckoutQuery.class));
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    mockMvc
        .perform(post("/subscriptions/0/reactivation-billing"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(post("/subscriptions/-1/reactivation-billing"))
        .andExpect(status().isBadRequest());
  }
}
