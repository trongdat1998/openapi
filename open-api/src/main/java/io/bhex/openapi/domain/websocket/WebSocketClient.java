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
import com.google.gson.JsonElement;
import io.bhex.base.account.BalanceDetail;
import io.bhex.base.account.BhTicketInfo;
import io.bhex.base.account.FuturesPosition;
import io.bhex.base.account.Order;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.openapi.domain.*;
import io.bhex.openapi.service.AccountService;
import io.bhex.openapi.service.OrderService;
import io.prometheus.client.Counter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.TextMessage;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WebSocketClient {
    /**
     * WebSocket connection belongs to a account, not belongs to a user.
     * So we mark subscribe channel with userId, not userId
     */

    private static final Counter COUNTER = Counter.build()
            .namespace("broker")
            .subsystem("stream")
            .name("socket_push_count")
            .labelNames("topic", "push_type")
            .help("Total number of http request started")
            .register();

    private static final String SOCKET_RECEIVE_TYPE = "receive";
    private static final String SOCKET_PUSH_TYPE = "push";

    /*
     * userId --> sessionIdList
     * A account can have multiple sessions, and a session can subscribe to different topic. Here, the topic collection of a certain account's session subscription is saved.
     * Key:     userId-sessionId
     * Value:   List<topic>
     */
    private static ConcurrentHashMap<Long, List<String>> accountSessionMap = new ConcurrentHashMap<>(1000);

    @Resource
    private OrderService orderService;

    @Resource
    private AccountService accountService;

    public void sysSubConnection(WebSocketSessionProxy sessionProxy) {
        Long[] accountIds = sessionProxy.getListenKeyResult().getAccountIds();
        for (Long accountId : accountIds) {
            if (accountSessionMap.containsKey(accountId)) {
                List<String> sessionIdList = accountSessionMap.getOrDefault(accountId, Lists.newArrayList());
                if (!sessionIdList.contains(sessionProxy.getSessionId())) {
                    sessionIdList.add(sessionProxy.getSessionId());
                    accountSessionMap.put(accountId, sessionIdList);
                }
            } else {
                accountSessionMap.put(accountId, Lists.newArrayList(sessionProxy.getSessionId()));
            }
        }
    }

    public void sysCancelConnection(WebSocketSessionProxy sessionProxy) {
        Long[] accountIds = sessionProxy.getListenKeyResult().getAccountIds();
        for (Long accountId : accountIds) {
            if (accountSessionMap.containsKey(accountId)) {
                List<String> sessionIdList = accountSessionMap.get(accountId);
                sessionIdList.remove(sessionProxy.getSessionId());
                if (sessionIdList.size() > 0) {
                    accountSessionMap.put(accountId, sessionIdList);
                } else {
                    accountSessionMap.remove(accountId);
                }
            }
        }
    }

    public boolean pushSubAccountInfoList(Long accountId, List<BalanceDetail> balanceDetails) {
        List<SocketAccountInfo> balanceResults = balanceDetails.stream().map(balanceDetail -> {
            SocketAccountInfo balanceResult = accountService.getSocketAccount(balanceDetail);
            if (getAccountType(balanceDetail.getAccountType()) == AccountType.MAIN) {
                balanceResult.setEventType("outboundAccountInfo");
            } else if (getAccountType(balanceDetail.getAccountType()) == AccountType.OPTION) {
                balanceResult.setEventType("outboundOptionAccountInfo");
            } else if (getAccountType(balanceDetail.getAccountType()) == AccountType.MARGIN) {
                balanceResult.setEventType("outboundMarginAccountInfo");
            } else {
                balanceResult.setEventType("outboundContractAccountInfo");
            }
            return balanceResult;
        }).collect(Collectors.toList());
        return pushSubMessage(accountId, TopicType.BALANCE, JsonUtil.defaultGson().toJsonTree(balanceResults));
    }

    public boolean pushSubOrderInfoList(Long accountId, List<Order> orders) {
        List<SocketOrderInfo> orderResults = new ArrayList<>();
        for (Order order : orders) {
            List<SocketOrderInfo> socketOrderInfos = orderService.getSocketOrderInfo(order);
            orderResults.addAll(socketOrderInfos.stream().peek(orderResult -> {
                if (getAccountType(order.getAccountType()) == AccountType.MAIN) {
                    orderResult.setEventType("executionReport");
                } else if (getAccountType(order.getAccountType()) == AccountType.OPTION) {
                    orderResult.setEventType("optionExecutionReport");
                } else if (getAccountType(order.getAccountType()) == AccountType.FUTURES) {
                    orderResult.setEventType("contractExecutionReport");
                } else if (getAccountType(order.getAccountType()) == AccountType.MARGIN) {
                    orderResult.setEventType("marginExecutionReport");
                }
            }).collect(Collectors.toList()));
        }
        return pushSubMessage(accountId, TopicType.ORDER, JsonUtil.defaultGson().toJsonTree(orderResults));
    }

    public boolean pushSubFuturesPositionInfo(Long accountId, List<FuturesPosition> positions) {
        List<SocketFuturesPositionInfo> positionInfos = positions.stream().map(position -> {
            SocketFuturesPositionInfo positionInfo = orderService.getSocketFuturesPositionInfo(position);
            positionInfo.setEventType("outboundContractPositionInfo");
            return positionInfo;
        }).collect(Collectors.toList());
        return pushSubMessage(accountId, TopicType.FUTURES_POSITION, JsonUtil.defaultGson().toJsonTree(positionInfos));
    }

    public boolean pubSubBhTicketInfoList(Long accountId, List<BhTicketInfo> bhTicketInfos) {
        List<SocketTicketInfo> socketTicketInfos = bhTicketInfos.stream().map(orderService::getSocketTicketInfo).collect(Collectors.toList());
        return pushSubMessage(accountId, TopicType.TICKET_INFO, JsonUtil.defaultGson().toJsonTree(socketTicketInfos));
    }

    private boolean pushSubMessage(Long accountId, TopicType topicType, JsonElement jsonMessage) {
        List<String> sessionIdList = accountSessionMap.get(accountId);
        if (CollectionUtils.isEmpty(sessionIdList)) {
//            log.warn("cannot find accountId:{} sessions...", accountId);
            return false;
        }
        for (String sessionId : sessionIdList) {
            WebSocketSessionProxy proxy = WebSocketSessionProxy.getInstanceBySessionId(sessionId);
            if (proxy != null && proxy.getWebSocketSession() != null && proxy.getWebSocketSession().isOpen()) {
                try {
                    synchronized (proxy.getWebSocketSession()) {
//                            PushMessage pushMessage = PushMessage.builder().topic(topicType.value()).data(jsonMessage).build();
                        proxy.getWebSocketSession().sendMessage(new TextMessage(JsonUtil.defaultGson().toJson(jsonMessage)));
//                        log.info("push accountId:{} {} message:{}", accountId, topicType.name(), JsonUtil.defaultGson().toJson(jsonMessage));
                    }
                } catch (IOException e) {
                    log.error("push accountId:{} {} message:{} failed", accountId, topicType, JsonUtil.defaultGson().toJson(jsonMessage), e);
                } catch (IllegalStateException e) {
                    log.warn("push accountId:{} {} message:{} failed has IllegalStateException",
                            accountId, topicType, JsonUtil.defaultGson().toJson(jsonMessage), e);
                }
            }
        }
        return true;
    }

    /**
     * get broker AccountType
     *
     * @param bhAccountType blueHelix account type
     * @return
     */
    private static AccountType getAccountType(Integer bhAccountType) {
        switch (bhAccountType) {
            case 23:
                return AccountType.OPTION;
            case 24:
                return AccountType.FUTURES;
            case 27:
                return AccountType.MARGIN;
            default:
                return AccountType.MAIN;
        }
    }

}
