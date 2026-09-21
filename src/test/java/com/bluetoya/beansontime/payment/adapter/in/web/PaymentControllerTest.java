package com.bluetoya.beansontime.payment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluetoya.beansontime.billing.domain.BillingId;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingCommand;
import com.bluetoya.beansontime.payment.application.port.in.PayBillingUseCase;
import com.bluetoya.beansontime.payment.application.port.in.PaymentResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PaymentControllerTest {

  @Test
  void paysByBillingIdWithoutAcceptingAnAmount() throws Exception {
    PayBillingUseCase useCase = mock(PayBillingUseCase.class);
    when(useCase.pay(new PayBillingCommand(new BillingId(11))))
        .thenReturn(
            new PaymentResult(
                21, 11, 30000, "SUCCESS", "transaction-1", LocalDateTime.of(2026, 9, 2, 10, 0)));
    PaymentController controller = new PaymentController(useCase);

    PaymentResponse response = controller.pay(11);

    assertThat(response.billingId()).isEqualTo(11);
    assertThat(response.amount()).isEqualTo(30000);
    assertThat(PaymentController.class.getDeclaredMethod("pay", long.class).getParameterCount())
        .isOne();
    verify(useCase).pay(new PayBillingCommand(new BillingId(11)));
  }

  @Test
  void rejectsZeroAndNegativeBillingIdsAsBadRequests() throws Exception {
    MockMvc mockMvc =
        MockMvcBuilders.standaloneSetup(new PaymentController(mock(PayBillingUseCase.class)))
            .build();

    mockMvc.perform(post("/billings/0/payments")).andExpect(status().isBadRequest());
    mockMvc.perform(post("/billings/-1/payments")).andExpect(status().isBadRequest());
  }
}
