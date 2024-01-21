package io.bhex.openapi.interceptor.annotation;

import io.bhex.openapi.domain.AccountType;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignAuth {
    boolean checkRecvWindow() default false;

    /**
     * 被允许的调用账户类型
     *
     * @return AccountType[]
     */
    AccountType[] requiredAccountTypes();

    boolean forceCheckIpWhiteList() default false;
}
