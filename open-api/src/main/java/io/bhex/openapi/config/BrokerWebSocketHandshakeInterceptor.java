/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.config
 *@Date 2018/7/9
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.config;

import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.entity.RequestPlatform;
import io.bhex.broker.common.util.HeaderUtil;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.openapi.domain.BrokerConstants;
import io.bhex.openapi.domain.BrokerInfo;
import io.bhex.openapi.domain.ValidListenKeyResult;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.service.UserDataStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
public class BrokerWebSocketHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    @Resource
    private BasicService basicService;

    @Resource
    UserDataStreamService userDataStreamService;

    @SuppressWarnings(value = "unchecked")
    @Override
    public boolean beforeHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse,
                                   WebSocketHandler handler, Map<String, Object> map) throws Exception {
        if (serverHttpRequest instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest request = (ServletServerHttpRequest) serverHttpRequest;

            Map<String, String> pathVariables = (Map<String, String>) request.getServletRequest().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            String listenKey = pathVariables.get("listenKey");

            Header header = HeaderUtil.buildFromHttp(request.getServletRequest());
            Header.Builder builder = header.toBuilder();
            builder.platform(RequestPlatform.OPENAPI);
            builder.language(LocaleContextHolder.getLocale().toString());

            String serverName = request.getServletRequest().getServerName();
            BrokerInfo brokerInfo = basicService.getByRequestHost(serverName);
            if (brokerInfo == null) {
                log.warn("websocket handshake with error broker info, return false");
                return false;
            }

            ValidListenKeyResult listenKeyResult = userDataStreamService.validListenKey(listenKey);
            if (listenKeyResult == null) {
                log.info("websocket handshake with error userId , listenKey is wrong:{}", listenKey);
                return false;
            }
            log.info("websocket handshake: listenKey:{} -> userInfo:{}", listenKey, JsonUtil.defaultGson().toJson(listenKey));
            builder.orgId(brokerInfo.getOrgId());
            header = builder.build();

            map.put(BrokerConstants.WEB_SOCKET_REQUEST_HEADER, header);
            map.put(BrokerConstants.WEB_SOCKET_LISTEN_KEY_HEADER, listenKeyResult);
//            map.put(BrokerConstants.WEB_SOCKET_REQUEST_USER_ID, userId);
//            map.put(BrokerConstants.WEB_SOCKET_CURRENT_LISTEN_KEY, listenKey);
            super.beforeHandshake(serverHttpRequest, serverHttpResponse, handler, map);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler handler, Exception e) {

    }

}
