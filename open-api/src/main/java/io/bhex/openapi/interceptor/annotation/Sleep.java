package io.bhex.openapi.interceptor.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sleep {
    int maxSleep();

    String exchangeType();
}
