/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.websocket
 *@Date 2018/8/14
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.websocket;

import com.google.common.collect.Lists;
import com.google.protobuf.TextFormat;
import io.bhex.base.account.*;
import io.bhex.base.proto.DecimalUtil;
import io.bhex.openapi.domain.BrokerConstants;
import io.bhex.openapi.domain.BrokerInfo;
import io.bhex.openapi.domain.websocket.WebSocketClient;
import io.bhex.openapi.grpc.config.GrpcClientConfig;
import io.bhex.openapi.service.BasicService;
import io.grpc.ManagedChannel;
import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReceiveStreamDataService implements DisposableBean {

    @Resource
    private WebSocketClient webSocketClient;

    @Resource
    private GrpcClientConfig grpcClientConfig;

    @Resource
    private BasicService basicService;

    @Resource
    TaskExecutor taskExecutor;

    private static final List<String> SUB_TOPICS = Lists.newArrayList("Order", "BalanceDetail", "BHTicketInfo");

    private static final Histogram pushMetrics = Histogram.build()
            .namespace("broker")
            .subsystem("stream")
            .name("stream_data_delay_milliseconds")
            .labelNames("stream_type")
            .buckets(BrokerConstants.CONTROLLER_TIME_BUCKETS)
            .help("Histogram of stream handle latency in milliseconds")
            .register();

    private boolean stopped = false;

    @EventListener(value = {ContextRefreshedEvent.class})
    public void init() {
        new Thread(this::startStream).start();
    }

    @Override
    public void destroy() throws Exception {
        this.stopped = true;
    }

    public void startStream() {
        while (!stopped) {
            try {
                log.info("Stream init......");
                doStartStream();
            } catch (Exception e) {
                log.error("stream subscribe process has error", e);
            }
        }
    }

    public void doStartStream() {
        List<Long> brokerIdList = basicService.queryAllBroker().stream().map(BrokerInfo::getOrgId).distinct().collect(Collectors.toList());
        subscribeServiceByOrgId(brokerIdList, 0L);
    }

    private void subscribeServiceByOrgId(List<Long> brokerIdList, Long orgId) {
        SubRequest request = SubRequest.newBuilder().addAllOrgId(brokerIdList).addAllTopics(SUB_TOPICS).build();
        SubscribeServiceGrpc.SubscribeServiceBlockingStub stub = grpcClientConfig.subscribeServiceBlockingStub(orgId);
        ManagedChannel channel = (ManagedChannel) stub.getChannel();
        Iterator<SubReply> replyIterator = stub.tradeStream(request);
        log.info("orgId:{} Stream subscribed", orgId);
        while (replyIterator.hasNext()) {
            SubReply reply = replyIterator.next();

            String streamType = reply.getType();
            long startTime = System.currentTimeMillis();

            try {
                if ("HeartBeat".equalsIgnoreCase(reply.getType())) {
                    log.info("orgId:{} Stream message HeartBeat......", orgId);
                } else if ("Order".equalsIgnoreCase(reply.getType())) {
                    Map<Long, List<Order>> pushMap = new HashMap<>();
                    for (Order order : reply.getOrdersList()) {
//                        log.info("orgId:{} Stream message Order, accountId:{}, orderId:{}, status:{}", orgId, order.getAccountId(), order.getOrderId(), order.getStatus().name());
                        if (pushMap.containsKey(order.getAccountId())) {
                            pushMap.get(order.getAccountId()).add(order);
                        } else {
                            pushMap.put(order.getAccountId(), Lists.newArrayList(order));
                        }
                        pushMetrics.labels(streamType).observe(startTime - order.getUpdatedTime());
                        pushMetrics.labels(streamType + "-create").observe(startTime - order.getCreatedTime());
                    }
                    CompletableFuture.runAsync(() -> {
                        for (Long accountId : pushMap.keySet()) {
                            if (webSocketClient.pushSubOrderInfoList(accountId, pushMap.get(accountId))) {
                                pushMetrics.labels(streamType + "-Push").observe(System.currentTimeMillis() - startTime);
                            }
                        }
                    }, taskExecutor);
                } else if ("BalanceDetail".equalsIgnoreCase(reply.getType())) {
                    Map<Long, List<BalanceDetail>> pushMap = new HashMap<>();
                    Map<Long, List<FuturesPosition>> pushMap2 = new HashMap<>();
                    for (BalanceDetail balanceDetail : reply.getBalancesList()) {
//                    BalanceDetail balanceDetail = reply.getBalances(0);
//                        log.info("orgId:{} Stream message BalanceDetail, accountId:{} tokenId:{} total:{}", orgId, balanceDetail.getAccountId(), balanceDetail.getTokenId(),
//                                DecimalUtil.toBigDecimal(balanceDetail.getTotal()).toPlainString());
                        pushMetrics.labels(streamType).observe(startTime - balanceDetail.getUpdatedAt());
                        if (pushMap.containsKey(balanceDetail.getAccountId())) {
                            pushMap.get(balanceDetail.getAccountId()).add(balanceDetail);
                        } else {
                            pushMap.put(balanceDetail.getAccountId(), Lists.newArrayList(balanceDetail));
                        }
                        if (balanceDetail.hasFuturesPosition()) {
                            pushMetrics.labels("FuturesPosition").observe(startTime - balanceDetail.getFuturesPosition().getUpdatedAt());
//                            log.info("orgId:{} Stream message FuturesPosition,accountId:{} position:{}", orgId, balanceDetail.getFuturesPosition().getAccountId(),
//                                    DecimalUtil.toBigDecimal(balanceDetail.getFuturesPosition().getTotal()).toPlainString());
                            if (pushMap2.containsKey(balanceDetail.getAccountId())) {
                                pushMap2.get(balanceDetail.getAccountId()).add(balanceDetail.getFuturesPosition());
                            } else {
                                pushMap2.put(balanceDetail.getAccountId(), Lists.newArrayList(balanceDetail.getFuturesPosition()));
                            }
                        }
                    }
                    CompletableFuture.runAsync(() -> {
                        for (Long accountId : pushMap.keySet()) {
                            if (webSocketClient.pushSubAccountInfoList(accountId, pushMap.get(accountId))) {
                                pushMetrics.labels(streamType + "-Push").observe(System.currentTimeMillis() - startTime);
                            }
                        }
                        for (Long accountId : pushMap2.keySet()) {
                            if (webSocketClient.pushSubFuturesPositionInfo(accountId, pushMap2.get(accountId))) {
                                pushMetrics.labels("FuturesPosition-Push").observe(System.currentTimeMillis() - startTime);
                            }
                        }
                    }, taskExecutor);
                } else if ("BHTicketInfo".equalsIgnoreCase(reply.getType())) {
                    Map<Long, List<BhTicketInfo>> pushMap = new HashMap<>();
                    for (BhTicketInfo bhTicketInfo : reply.getTicketsList()) {
                        pushMetrics.labels(streamType).observe(startTime - bhTicketInfo.getMatchTime());
//                        log.info("orgId:{} Stream message BhTicketInfo: {}", orgId, TextFormat.shortDebugString(bhTicketInfo));
                        if (pushMap.containsKey(bhTicketInfo.getAccountId())) {
                            pushMap.get(bhTicketInfo.getAccountId()).add(bhTicketInfo);
                        } else {
                            pushMap.put(bhTicketInfo.getAccountId(), Lists.newArrayList(bhTicketInfo));
                        }
                    }
                    CompletableFuture.runAsync(() -> {
                        for (Long accountId : pushMap.keySet()) {
                            if (webSocketClient.pubSubBhTicketInfoList(accountId, pushMap.get(accountId))) {
                                pushMetrics.labels(streamType + "-Push").observe(System.currentTimeMillis() - startTime);
                            }
                        }
                    }, taskExecutor);
                }
                if (stopped) {
                    log.warn("orgId:{} server stopped, so stop stream", orgId);
                    break;
                }
                if (channel.isShutdown()) {
                    log.warn("orgId:{} channel shutdown, so stop stream", orgId);
                    break;
                }
            } catch (Exception e) {
                log.warn("orgId:{} Stream onNext occurred a Exception, but I cannot throw it, so sad!", orgId, e);
            }
        }
        log.error("orgId:{} Stream has no message, maybe is shutdown, restart a stream client", orgId);
    }

}
