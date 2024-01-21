/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain.websocket
 *@Date 2018/7/10
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain.websocket;

import com.google.common.collect.Lists;
import io.bhex.broker.common.entity.Header;
import io.bhex.openapi.domain.AccountType;
import io.bhex.openapi.domain.ValidListenKeyResult;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Data
public final class WebSocketSessionProxy {

    private static final Long EXPIRE_TIMESTAMP = 60 * 60 * 1000L;

    private static final ConcurrentHashMap<String, WebSocketSessionProxy> SESSION_MAP = new ConcurrentHashMap<>(1000);

    private static final ConcurrentHashMap<Long, List<String>> ACCOUNT_ID_SESSION_IDS_MAP = new ConcurrentHashMap<>(1000);

    private Header handshakeHeader;
    private ValidListenKeyResult listenKeyResult;
    private String sessionId;
    private WebSocketSession webSocketSession;
    private Long expireTimestamp;
    private String listenKey;

    private WebSocketSessionProxy(Header header, ValidListenKeyResult listenKeyResult, WebSocketSession webSocketSession) {
        this.handshakeHeader = header;
        this.listenKeyResult = listenKeyResult;
        this.sessionId = webSocketSession.getId();
        this.webSocketSession = webSocketSession;
        this.listenKey = listenKeyResult.getListenKey();
    }

    public static WebSocketSessionProxy getInstance(Header handshakeRequest, WebSocketSession webSocketSession, ValidListenKeyResult listenKeyResult) {
        WebSocketSessionProxy proxy = new WebSocketSessionProxy(handshakeRequest, listenKeyResult, webSocketSession);
        proxy.setExpireTimestamp(System.currentTimeMillis() + EXPIRE_TIMESTAMP);
        WebSocketSessionProxy existProxy = SESSION_MAP.putIfAbsent(webSocketSession.getId(), proxy);
        if (existProxy != null) {
            proxy = existProxy;
        }
        return proxy;
    }

    public static WebSocketSessionProxy getInstanceBySessionId(String sessionId) {
        return SESSION_MAP.get(sessionId);
    }

    public static void extendSession(Long accountId) {
        List<String> sessionIds = ACCOUNT_ID_SESSION_IDS_MAP.getOrDefault(accountId, Lists.newArrayList());
        if (!CollectionUtils.isEmpty(sessionIds)) {
            WebSocketSessionProxy proxy;
            for (String sessionId : sessionIds) {
                proxy = SESSION_MAP.get(sessionId);
                if (proxy != null) {
                    proxy.setExpireTimestamp(System.currentTimeMillis() + EXPIRE_TIMESTAMP);
                    SESSION_MAP.put(sessionId, proxy);
                }
            }
        }
    }

    public static WebSocketSessionProxy remove(String sessionId) {
        return SESSION_MAP.remove(sessionId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WebSocketSessionProxy)) {
            return false;
        }
        WebSocketSessionProxy target = (WebSocketSessionProxy) obj;
        return (this.sessionId == null ? target.sessionId == null : this.sessionId.equals(target.sessionId));
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this.webSocketSession, false);
    }

}
