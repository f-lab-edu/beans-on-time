package com.bluetoya.beansontime.billing.application.port.in;

public interface PrepareReactivationBillingUseCase {
  PreparedBillingDetail prepare(PrepareReactivationBillingCommand command);
}
