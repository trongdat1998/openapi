package io.bhex.openapi;

import io.bhex.base.grpc.client.GrpcClientAutoConfiguration;
import io.bhex.base.grpc.client.channel.IGrpcClientPool;
import io.bhex.base.grpc.metrics.TomcatConnectPoolMetricsSupport;
import io.bhex.base.idgen.api.ISequenceGenerator;
import io.bhex.base.idgen.enums.DataCenter;
import io.bhex.base.idgen.enums.ServiceID;
import io.bhex.base.idgen.snowflake.SnowflakeGenerator;
import io.bhex.base.metrics.PrometheusMetricsCollector;
import io.bhex.broker.common.grpc.client.aspect.GrpcLogAspect;
import io.bhex.broker.common.grpc.client.aspect.PrometheusMetricsAspect;
import io.bhex.broker.common.redis.GsonValueSerializer;
import io.bhex.broker.common.redis.StringKeySerializer;
import io.bhex.broker.core.domain.BrokerCoreConstants;
import io.bhex.broker.core.interceptor.HeaderInterceptor;
import io.bhex.openapi.grpc.config.GrpcClientConfig;
import io.bhex.openapi.interceptor.OpenApiInterceptor;
import io.bhex.openapi.interceptor.TestOpenApiInterceptor;
import io.lettuce.core.ReadFrom;
import io.prometheus.client.hotspot.DefaultExports;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;

/************************************
 * @项目名称: broker
 * @文件名称: BrokerServer
 * @Date 2018/05/22
 * @Author will.zhao@bhex.io
 * @Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 **************************************
 */
@SpringBootApplication(exclude = {GrpcClientAutoConfiguration.class}, scanBasePackages = {"io.bhex"})
@EnableScheduling
@EnableAsync
@Slf4j
public class BrokerApplication {

    static {
//        System.setProperty("io.netty.maxDirectMemory", String.valueOf(1024 * 1024 * 1024L));
        DefaultExports.initialize();
    }

    @Bean
    public BrokerInitializer brokerInitializer() {
        return new BrokerInitializer();
    }

    @Bean
    public BrokerProperties brokerProperties() {
        return new BrokerProperties();
    }

    @Bean
    public ISequenceGenerator sequenceGenerator() {
        return SnowflakeGenerator.newInstance(DataCenter.DC1.value(), ServiceID.BROKER.value());
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("TaskScheduler-");
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(1000);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("taskExecutor-");
        executor.setAwaitTerminationSeconds(10);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Bean
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new ConcurrentTaskExecutor(taskExecutor());
    }

    @Bean
    public GrpcLogAspect grpcLogAspect() {
        return new GrpcLogAspect();
    }

    @Bean
    public PrometheusMetricsAspect prometheusMetricsAspect() {
        return new PrometheusMetricsAspect();
    }

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceCustomizer() {
//        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
////                .enablePeriodicRefresh()
////                .enablePeriodicRefresh(Duration.ofSeconds(5))
//                .enableAllAdaptiveRefreshTriggers()
//                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(5))
//                .build();
//
//        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
//                .topologyRefreshOptions(topologyRefreshOptions)
//                .build();
//        return clientConfigurationBuilder -> clientConfigurationBuilder.clientOptions(clientOptions);

        return clientConfigurationBuilder -> clientConfigurationBuilder.readFrom(ReadFrom.REPLICA_PREFERRED);
    }

    @Bean
    public HeaderInterceptor headerInterceptor(IGrpcClientPool pool) {
        return new HeaderInterceptor(pool, GrpcClientConfig.BROKER_SERVER_CHANNEL_NAME);
    }

    @Bean
    public OpenApiInterceptor openApiInterceptor() {
        return new OpenApiInterceptor();
    }

    @Bean
    public TestOpenApiInterceptor testOpenApiInterceptor() {
        return new TestOpenApiInterceptor();
    }

    @Bean("brokerRedisTemplate")
    @SuppressWarnings(value = {"unchecked"})
    public RedisTemplate<String, String> brokerRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringKeySerializer("api-"));
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

    @Bean("authorizeRedisTemplate")
    public RedisTemplate<String, Long> authorizeRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringKeySerializer(BrokerCoreConstants.AUTHORIZE_KEY_PREFIX));
        redisTemplate.setValueSerializer(new GsonValueSerializer<>(Long.class));
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> tomcatPoolMetricsSupport(TomcatConnectPoolMetricsSupport tomcatConnectPoolMetricsSupport) {
        return factory -> {
            if (factory instanceof TomcatServletWebServerFactory) {
                log.info("Customizer for add Tomcat connect pool metrics support");
                ((TomcatServletWebServerFactory) factory).addConnectorCustomizers(tomcatConnectPoolMetricsSupport);
            } else {
                log.warn("NOT Tomcat, so do not add connect pool metrics support");
            }
        };
    }

    /**
     * do not use ContextRefreshEvent, because tomcat init executor after that event
     *
     * @param applicationContext
     * @return
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> registerPoolMetrics(ApplicationContext applicationContext) {
        return event -> {
            log.info("[INFO] on ApplicationReadyEvent, add all pool to metrics ");
            Map<String, ThreadPoolTaskExecutor> taskExecutorMap = applicationContext.getBeansOfType(ThreadPoolTaskExecutor.class);
            for (String name : taskExecutorMap.keySet()) {
                ThreadPoolTaskExecutor executor = taskExecutorMap.get(name);
                log.info("register to metrics ThreadPoolTaskExecutor: {} = {}", name, executor);
                PrometheusMetricsCollector.registerThreadPoolExecutor(executor.getThreadPoolExecutor(), name);
            }

            Map<String, TomcatConnectPoolMetricsSupport> tomcatMetricsSupports = applicationContext.getBeansOfType(TomcatConnectPoolMetricsSupport.class);
            for (TomcatConnectPoolMetricsSupport support : tomcatMetricsSupports.values()) {
                log.info("addTomcatConnectPoolMetrics for: {} ", support);
                support.addTomcatConnectPoolMetrics();
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(BrokerApplication.class, args);
    }

}

