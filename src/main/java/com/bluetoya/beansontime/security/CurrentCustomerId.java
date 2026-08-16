package com.bluetoya.beansontime.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "customerId", errorOnInvalidType = true)
public @interface CurrentCustomerId {
}
