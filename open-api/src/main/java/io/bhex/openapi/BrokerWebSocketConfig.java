/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker
 *@Date 2018/7/30
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi;

import io.bhex.openapi.config.BrokerWebSocketHandler;
import io.bhex.openapi.config.BrokerWebSocketHandshakeInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocket
public class BrokerWebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    @Bean
    public BrokerWebSocketHandler brokerWebSocketHandler() {
        return new BrokerWebSocketHandler();
    }

    @Bean
    public BrokerWebSocketHandshakeInterceptor brokerWebSocketHandshakeInterceptor() {
        return new BrokerWebSocketHandshakeInterceptor();
    }

    @SuppressWarnings(value = {"NullableProblems"})
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(brokerWebSocketHandler(), "/openapi/ws/{listenKey}").setAllowedOrigins("*")
                .addInterceptors(brokerWebSocketHandshakeInterceptor());
        registry.addHandler(brokerWebSocketHandler(), "/openapi/sockjs/user").setAllowedOrigins("*")
                .addInterceptors(brokerWebSocketHandshakeInterceptor()).withSockJS();
    }

}
