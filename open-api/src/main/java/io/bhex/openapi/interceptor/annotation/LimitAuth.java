package io.bhex.openapi.interceptor.annotation;

import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.domain.api.enums.RateLimitWeightType;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LimitAuth {


    RateLimitType[] limitTypes() default {};

    int weight() default 0;

    RateLimitWeightType weightType() default RateLimitWeightType.NORMAL;
}
