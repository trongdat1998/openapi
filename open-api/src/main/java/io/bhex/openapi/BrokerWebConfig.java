/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker
 *@Date 2018/6/21
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi;

import com.google.common.collect.Lists;
import io.bhex.broker.core.config.HeaderArgumentResolver;
import io.bhex.broker.core.interceptor.HeaderInterceptor;
import io.bhex.broker.core.interceptor.PrometheusInterceptor;
import io.bhex.openapi.interceptor.OpenApiInterceptor;
import io.bhex.openapi.interceptor.TestOpenApiInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;

@Slf4j
@Configuration
@EnableWebSocket
public class BrokerWebConfig extends WebMvcConfigurationSupport {

    @Resource
    private HeaderInterceptor headerInterceptor;

    @Resource
    private OpenApiInterceptor openApiInterceptor;

    @Resource
    private TestOpenApiInterceptor testOpenApiInterceptor;

    @Override
    protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("asyncTaskExecutor-");
        executor.setAwaitTerminationSeconds(10);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        configurer.setTaskExecutor(executor);
        super.configureAsyncSupport(configurer);
    }

    @Bean
    public FilterRegistrationBean<FormContentFilter> traceFilterRegistration() {
        FilterRegistrationBean<FormContentFilter> registration = new FilterRegistrationBean<>();
        registration.setName("formContentFilter");
        registration.setFilter(new FormContentFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Override
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setSupportedLocales(Lists.newArrayList(Locale.US, Locale.CHINA));
        localeResolver.setDefaultLocale(Locale.US);
        return localeResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PrometheusInterceptor()).excludePathPatterns("/internal/**");
        registry.addInterceptor(headerInterceptor).excludePathPatterns("/internal/**");
        registry.addInterceptor(openApiInterceptor).excludePathPatterns("/internal/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HeaderArgumentResolver());
    }

}
