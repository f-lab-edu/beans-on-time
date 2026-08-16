package com.bluetoya.beansontime.security;

import java.lang.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "customerId", errorOnInvalidType = true)
public @interface CurrentCustomerId {}
