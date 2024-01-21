/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.config
 *@Date 2018/7/9
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.openapi.domain.BrokerConstants;
import io.bhex.openapi.domain.ValidListenKeyResult;
import io.bhex.openapi.domain.websocket.WebSocketClient;
import io.bhex.openapi.domain.websocket.WebSocketSessionProxy;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * websocket handler
 */
@Slf4j
public class BrokerWebSocketHandler implements WebSocketHandler {

    private static final String PING_MSG_KEY = "ping";

    private static final String PONG_MSG_KEY = "pong";

    private static ConcurrentHashMap<String, Long> activeSessionMap = new ConcurrentHashMap<>(5000);

    private static final Gauge GAUGE = Gauge.build()
            .namespace("broker")
            .subsystem("web_socket")
            .name("connect_total")
            .labelNames("client_type")
            .help("Total number of http request started")
            .register();

    private static final Histogram HISTOGRAM = Histogram.build()
            .namespace("broker")
            .subsystem("web_socket")
            .name("client_delay_milliseconds")
            .labelNames("client_type")
            .buckets(BrokerConstants.CONTROLLER_TIME_BUCKETS)
            .help("Histogram of websocket client handle latency in milliseconds")
            .register();

    private static final Integer SESSION_TIMEOUT = 60 * 60 * 1000;

    @Resource
    private WebSocketClient webSocketClient;

    @Scheduled(cron = "0/30 * * * * ?")
    public void pingSession() {
        activeSessionMap.keySet().parallelStream()
                .forEach(sessionId -> {
                    WebSocketSessionProxy proxy = WebSocketSessionProxy.getInstanceBySessionId(sessionId);
                    if (proxy != null && proxy.getWebSocketSession() != null && proxy.getWebSocketSession().isOpen()) {
                        JsonObject pingData = new JsonObject();
                        pingData.addProperty(PING_MSG_KEY, System.currentTimeMillis());
                        sendHeartBeatMessage(proxy.getWebSocketSession(), pingData);
                    }
                });
    }

    @Scheduled(cron = "0 0/60 * * * ?")
    public void killSession() {
        activeSessionMap.keySet().parallelStream()
                .forEach(sessionId -> {
                    WebSocketSessionProxy proxy = WebSocketSessionProxy.getInstanceBySessionId(sessionId);
                    if (proxy != null) {
                        long lastActiveTime = activeSessionMap.get(sessionId);
                        if (System.currentTimeMillis() - lastActiveTime > SESSION_TIMEOUT
                                && proxy.getWebSocketSession() != null && proxy.getWebSocketSession().isOpen()) {
                            try {
                                proxy.getWebSocketSession().close();
                                log.info("kill no heartbeat session(listenKeyResult:{}, sessionId:{})", proxy.getListenKeyResult(), proxy.getSessionId());
                                // now afterConnectionClosed will be invoked
                                activeSessionMap.remove(sessionId);
                            } catch (Exception e) {
                                log.error("close inactive session:{} failed", proxy.getSessionId());
                            }
                        }
                    }
                });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Header handshakeHeader = (Header) session.getAttributes().get(BrokerConstants.WEB_SOCKET_REQUEST_HEADER);
        ValidListenKeyResult listenKeyResult = (ValidListenKeyResult) session.getAttributes().get(BrokerConstants.WEB_SOCKET_LISTEN_KEY_HEADER);

        if (listenKeyResult == null) {
            log.info("after connection established, close session(sessionId:{}) because required session attribute listenKey is null", session.getId());
            session.close();
            return;
        }
        GAUGE.labels(handshakeHeader.getPlatform().name()).inc();
        WebSocketSessionProxy proxy = WebSocketSessionProxy.getInstance(handshakeHeader, session, listenKeyResult);
        activeSessionMap.put(session.getId(), System.currentTimeMillis());
        webSocketClient.sysSubConnection(proxy);
        log.debug("userId: ({}) connection established", JsonUtil.defaultGson().toJson(listenKeyResult));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        activeSessionMap.put(session.getId(), System.currentTimeMillis());
        WebSocketSessionProxy proxy = WebSocketSessionProxy.getInstanceBySessionId(session.getId());
        if (proxy == null || proxy.getWebSocketSession() == null || !proxy.getWebSocketSession().isOpen()) {
            return;
        }
        if (message instanceof PingMessage) {
            JsonObject pongData = new JsonObject();
            pongData.addProperty(PONG_MSG_KEY, System.currentTimeMillis());
            sendHeartBeatMessage(session, pongData);
        } else if (message instanceof PongMessage) {
            JsonObject pingData = new JsonObject();
            pingData.addProperty(PING_MSG_KEY, System.currentTimeMillis());
            sendHeartBeatMessage(session, pingData);
        } else if (message instanceof TextMessage) {
            String messageBody = ((TextMessage) message).getPayload();
            JsonElement json = JsonUtil.defaultJsonParser().parse(messageBody);
            if (json.isJsonNull() || json.isJsonPrimitive()) {
                return;
            }
            if (messageBody.contains(PING_MSG_KEY)) {
                JsonObject pongData = new JsonObject();
                pongData.addProperty(PONG_MSG_KEY, JsonUtil.getLong(json, ".ping", System.currentTimeMillis()));
                sendHeartBeatMessage(session, pongData);
            } else if (messageBody.contains(PONG_MSG_KEY)) {
                Long pingTimestamp = JsonUtil.getLong(json, ".pong", System.currentTimeMillis());
                Long delay = System.currentTimeMillis() - pingTimestamp;
                HISTOGRAM.labels(proxy.getHandshakeHeader().getPlatform().name()).observe(delay);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) throws Exception {
        WebSocketSessionProxy proxy = WebSocketSessionProxy.getInstanceBySessionId(session.getId());
        if (proxy != null) {
            log.warn("cache a transport error with websocket connection(listenKeyResult:{}, sessionId:{}), session is open ? {}",
                    proxy.getListenKeyResult(), session.getId(), proxy.getWebSocketSession().isOpen());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        WebSocketSessionProxy proxy = WebSocketSessionProxy.getInstanceBySessionId(session.getId());
        if (proxy != null) {
            webSocketClient.sysCancelConnection(proxy);
            GAUGE.labels(proxy.getHandshakeHeader().getPlatform().name()).dec();
            log.info("websocket connection(listenKey:{}, sessionId:{}) closed......", proxy.getListenKeyResult(), session.getId());
            if (session.isOpen()) {
                session.close();
            }
            WebSocketSessionProxy.remove(session.getId());
        }
        activeSessionMap.remove(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private void sendHeartBeatMessage(WebSocketSession session, JsonObject jsonMessage) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(JsonUtil.defaultGson().toJson(jsonMessage)));
                }
                activeSessionMap.put(session.getId(), System.currentTimeMillis());
            }
        } catch (IOException e) {
            if (!session.isOpen()) {
                log.warn("send WS message({}), but session was closed", jsonMessage);
            } else {
                log.error("send WS message({}) IoException", jsonMessage, e);
            }
        } catch (IllegalStateException e) {
            log.warn("send heartbeat message has IllegalStateException", e);
        }
    }

}
