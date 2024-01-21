package io.bhex.openapi.service;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.protobuf.TextFormat;
import io.bhex.base.proto.OrderStatusEnum;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.order.OrderSide;
import io.bhex.broker.grpc.order.*;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.*;
import io.bhex.openapi.domain.api.enums.ApiFuturesOrderType;
import io.bhex.openapi.domain.api.enums.ApiFuturesPositionSide;
import io.bhex.openapi.domain.api.enums.ApiFuturesTimeInForce;
import io.bhex.openapi.domain.api.result.*;
import io.bhex.openapi.grpc.client.GrpcFuturesOrderService;
import io.bhex.openapi.grpc.client.GrpcOrderService;
import io.bhex.openapi.util.FuturesTools;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class FuturesOrderService {

    private static final int MAX_BATCH_CANCEL_ORDER_SIZE = 100;
    private static final int DEFAULT_QUERY_LIMIT = 100;

    @Resource
    private GrpcFuturesOrderService grpcFuturesOrderService;

    @Resource
    private GrpcOrderService grpcOrderService;

    @Resource
    private BasicService basicService;

    public FuturesOrderResult newFuturesOrder(Header header, Long exchangeId, String symbolId, Long accountId,
                                              AccountTypeEnum accountType, Integer accountIndex,
                                              String clientOrderId, String orderSide, String orderType, String price,
                                              String triggerPrice, String priceType, String quantity,
                                              String timeInForce, String leverage, String methodVersion, String orderSource) {
        FuturesOrderSide futuresOrderSide = FuturesOrderSide.valueOf(orderSide);
        PlanOrder.FuturesOrderType futuresOrderType = FuturesTools.toFuturesOrderType(orderType);
        boolean isClose = futuresOrderSide == FuturesOrderSide.BUY_CLOSE
                || futuresOrderSide == FuturesOrderSide.SELL_CLOSE;

        CreateFuturesOrderRequest.Builder requestBuilder = CreateFuturesOrderRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setSymbolId(symbolId)
                .setQuantity(quantity)
                .setExchangeId(exchangeId)
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setClientOrderId(clientOrderId)
                .setFuturesOrderSide(FuturesOrderSide.valueOf(orderSide))
                .setPrice(price)
                .setFuturesPriceType(FuturesTools.toFuturesPriceType(priceType))
                .setFuturesOrderType(futuresOrderType)
                .setIsClose(isClose)
                .setLeverage(leverage)
                .setTriggerPrice(triggerPrice)
                .setOrderSource(orderSource);

        if (Strings.nullToEmpty(orderType).toUpperCase().equals(ApiFuturesOrderType.LIMIT_MAKER.name())) {
            requestBuilder.setExtraFlag(FuturesOrderExtraFlag.MAKER_ONLY);
        }
        if (timeInForce.equals(ApiFuturesTimeInForce.LIMIT_MAKER.name())) {
            requestBuilder.setExtraFlag(FuturesOrderExtraFlag.MAKER_ONLY);
        } else {
            requestBuilder.setTimeInForce(OrderTimeInForceEnum.valueOf(timeInForce));
        }

        // 做市程序专用订单类型
        if (orderType.equals(ApiFuturesOrderType.LIMIT_FREE.name())) {
            requestBuilder.setExtraFlag(FuturesOrderExtraFlag.LIMIT_FREE_FLAG);
        }
        if (orderType.equals(ApiFuturesOrderType.LIMIT_MAKER_FREE.name())) {
            requestBuilder.setExtraFlag(FuturesOrderExtraFlag.LIMIT_MAKER_FREE_FLAG);
        }

        if (futuresOrderType == PlanOrder.FuturesOrderType.STOP) {
            // 如果是下计划委托单，planOrderType默认为STOP_COMMON
            requestBuilder.setPlanOrderType(PlanOrder.PlanOrderTypeEnum.STOP_COMMON);
        }

        CreateFuturesOrderRequest request = requestBuilder.build();
//        CreateFuturesOrderResponse response = null;
//        if (!Strings.isNullOrEmpty(methodVersion) && methodVersion.equals("2.0")) {
//            response = grpcFuturesOrderService.createFuturesOrderV20(request);
//        } else if (!Strings.isNullOrEmpty(methodVersion) && methodVersion.equals("2.1")) {
//            response = grpcFuturesOrderService.createFuturesOrderV21(request);
//        } else {
//            response = grpcFuturesOrderService.createFuturesOrder(request);
//        }
        CreateFuturesOrderResponse response = grpcFuturesOrderService.createFuturesOrder(request);

        if (futuresOrderType == PlanOrder.FuturesOrderType.STOP) {
            if (!response.hasPlanOrder()) {
                throw new BrokerException(BrokerErrorCode.ORDER_FAILED,
                        new RuntimeException("response has no plan order field"));
            }

            return getFuturesPlanOrderResult(response.getPlanOrder());
        } else {
            if (!response.hasOrder()) {
                throw new BrokerException(BrokerErrorCode.ORDER_FAILED,
                        new RuntimeException("response has no order field"));
            }

            Order order = response.getOrder();
            if (order.getStatusCode().equals(OrderStatusEnum.REJECTED.name())) {
                log.warn("newFuturesOrder: REJECTED (request: {})", TextFormat.shortDebugString(request));
                throw new BrokerException(BrokerErrorCode.ORDER_FAILED, new RuntimeException("match rejected"));
            }

            return getFuturesOrderResult(order);
        }
    }

    public FuturesOrderResult cancelFuturesOrder(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                                 Long orderId, String clientOrderId, String futuresOrderType, String methodVersion) {

        PlanOrder.FuturesOrderType orderType = FuturesTools.toFuturesOrderType(futuresOrderType);

        CancelFuturesOrderRequest.Builder requestBuilder = CancelFuturesOrderRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setFuturesOrderType(orderType);

        if (orderId != null && orderId > 0) {
            requestBuilder.setOrderId(orderId);
        }

        if (StringUtils.isNotEmpty(clientOrderId)) {
            requestBuilder.setClientOrderId(clientOrderId);
        }

//        CancelFuturesOrderResponse response = null;
//        if (!Strings.isNullOrEmpty(methodVersion) && methodVersion.equals("2.0")) {
//            response = grpcFuturesOrderService.cancelFuturesOrderV20(requestBuilder.build());
//        } else if (!Strings.isNullOrEmpty(methodVersion) && methodVersion.equals("2.1")) {
//            response = grpcFuturesOrderService.cancelFuturesOrderV21(requestBuilder.build());
//        } else {
//            response = grpcFuturesOrderService.cancelFuturesOrder(requestBuilder.build());
//        }
        CancelFuturesOrderResponse response = grpcFuturesOrderService.cancelFuturesOrder(requestBuilder.build());
        if (orderType == PlanOrder.FuturesOrderType.STOP) {
            if (response.hasPlanOrder()) {
                return getFuturesPlanOrderResult(response.getPlanOrder());
            } else {
                return getFuturesPlanOrder(header, accountId, accountType, accountIndex, orderId, clientOrderId);
            }
        } else {
            if (response.hasOrder()) {
                return getFuturesOrderResult(response.getOrder());
            } else {
                return getFuturesOrder(header, accountId, accountType, accountIndex, orderId, clientOrderId);
            }
        }
    }

    public void batchCancelFuturesOrder(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                        List<String> symbolIdList, String orderSide) {
        io.bhex.broker.grpc.order.BatchCancelOrderRequest.Builder builder
                = io.bhex.broker.grpc.order.BatchCancelOrderRequest.newBuilder();

        builder.addAllSymbolIds(symbolIdList);
        if (StringUtils.isNotEmpty(orderSide)) {
            builder.setOrderSide(OrderSide.valueOf(orderSide.toUpperCase()));
        }
        builder.setHeader(HeaderConvertUtil.convertHeader(header));
        builder.setAccountId(accountId).setAccountType(accountType).setAccountIndex(accountIndex);
        grpcFuturesOrderService.batchCancelFutureOrder(builder.build());
    }

//    public void batchCancelFuturesOrder(Header header, Long accountId, List<Long> orderIdList, List<String> clientOrderIdList) {
//        if (orderIdList.size() > MAX_BATCH_CANCEL_ORDER_SIZE
//                || clientOrderIdList.size() > MAX_BATCH_CANCEL_ORDER_SIZE) {
//            throw new OpenApiException(ApiErrorCode.TOO_MANY_PARAMETERS);
//        }
//
//        String futuresOrderType = "LIMIT";
//
//        if (!orderIdList.isEmpty()) {
//            orderIdList.forEach(orderId -> CompletableFuture.runAsync(
//                    () -> cancelFuturesOrder(header, orderId, null, futuresOrderType)));
//        } else {
//            clientOrderIdList.forEach(clientOrderId -> CompletableFuture.runAsync(
//                    () -> cancelFuturesOrder(header, null, clientOrderId, futuresOrderType)));
//        }
//    }

    public List<FuturesOrderResult> queryFuturesOrders(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                                       String futuresOrderType, String symbolId, Long orderId, Integer limit, boolean isHistory) {
        PlanOrder.FuturesOrderType orderType = PlanOrder.FuturesOrderType.valueOf(futuresOrderType);
        OrderQueryType orderQueryType = isHistory ? OrderQueryType.HISTORY : OrderQueryType.CURRENT;
        QueryFuturesOrdersRequest.Builder requestBuilder = QueryFuturesOrdersRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setSymbolId(symbolId)
                .setLimit(limit)
                .setOrderType(orderType)
                .setQueryType(orderQueryType);
        if (orderId != null && orderId > 0) {
            requestBuilder.setFromId(orderId);
        }

        QueryFuturesOrdersResponse response = grpcFuturesOrderService.queryFuturesOrders(requestBuilder.build());
        switch (orderType) {
            case STOP:
                return response.getPlanOrdersList().stream().map(this::getFuturesPlanOrderResult).collect(Collectors.toList());
            case LIMIT:
                return response.getOrdersList().stream().map(this::getFuturesOrderResult).collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }

    public List<FuturesMatchResult> queryFuturesMatchInfo(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                                          String symbolId, Long fromId, Long toId, Integer limit) {
        QueryMatchRequest.Builder requestBuilder = QueryMatchRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setSymbolId(symbolId)
                .setFromId(fromId)
                .setEndId(toId)
                .setStartTime(0L)
                .setEndTime(0L)
                .setLimit(limit);
//        if (StringUtils.isNotEmpty(futuresOrderSide)) {
//            requestBuilder.setOrderSide(FuturesTools.getOrderSide(FuturesOrderSide.valueOf(futuresOrderSide)));
//        }
        QueryMatchResponse response = grpcFuturesOrderService.queryFuturesMatchInfo(requestBuilder.build());
        return response.getMatchList().stream().map(m -> getMatchResult(header, m)).collect(Collectors.toList());
    }

    public List<FuturesPositionResult> getFuturesPositions(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                                           List<String> symbolIdList, String positionSide) {
        FuturesPositionsRequest request = FuturesPositionsRequest
                .newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .addAllTokenIds(symbolIdList)
                .setLimit(DEFAULT_QUERY_LIMIT)
                .build();
        FuturesPositionsResponse response = grpcFuturesOrderService.getFuturesPositions(request);
        Stream<FuturesPosition> positionStream = response.getPositionsList().stream()
                // 过滤掉total为0的持仓数据
                .filter(t -> new BigDecimal(t.getTotal()).compareTo(BigDecimal.ZERO) != 0);
        if (StringUtils.isNotEmpty(positionSide)) {
            String filterIsLong = positionSide.equals(ApiFuturesPositionSide.LONG.name()) ? "1" : "0";
            positionStream = positionStream.filter(t -> t.getIsLong().equals(filterIsLong));
        }
        return positionStream.map(FuturesTools::toFuturesPositionResult).collect(Collectors.toList());
    }

    public ModifyMarginResult modifyMargin(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                           String symbolId, String positionSide, BigDecimal amount) {
        boolean isLong = FuturesTools.toBrokerPositionLongBool(positionSide);
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            AddMarginRequest request = AddMarginRequest.newBuilder()
                    .setHeader(HeaderConvertUtil.convertHeader(header))
                    .setAccountId(accountId)
                    .setAccountType(accountType)
                    .setAccountIndex(accountIndex)
                    .setSymbolId(symbolId)
                    .setIsLong(isLong)
                    .setAmount(amount.toPlainString())
                    .build();
            AddMarginResponse response = grpcFuturesOrderService.addMargin(request);
            if (!response.getSuccess()) {
                throw new OpenApiException(ApiErrorCode.ERROR_MODIFY_MARGIN);
            }
        } else {
            ReduceMarginRequest request = ReduceMarginRequest.newBuilder()
                    .setHeader(HeaderConvertUtil.convertHeader(header))
                    .setAccountId(accountId)
                    .setAccountType(accountType)
                    .setAccountIndex(accountIndex)
                    .setSymbolId(symbolId)
                    .setIsLong(isLong)
                    .setAmount(amount.negate().toPlainString())
                    .build();
            ReduceMarginResponse response = grpcFuturesOrderService.reduceMargin(request);
            if (!response.getSuccess()) {
                throw new OpenApiException(ApiErrorCode.ERROR_MODIFY_MARGIN);
            }
        }

        String isLongString = FuturesTools.toBrokerPositionLongString(positionSide);
        FuturesPositionsRequest positionsRequest = FuturesPositionsRequest
                .newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .addAllTokenIds(Collections.singletonList(symbolId))
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setIsLong(isLongString)
                .build();
        FuturesPositionsResponse positionsResponse = grpcFuturesOrderService.getFuturesPositions(positionsRequest);
        for (FuturesPosition futuresPosition : positionsResponse.getPositionsList()) {
            if (futuresPosition.getTokenId().equals(symbolId) && futuresPosition.getIsLong().equals(isLongString)) {
                return ModifyMarginResult.builder()
                        .margin(futuresPosition.getMargin())
                        .symbol(symbolId)
                        .timestamp(System.currentTimeMillis())
                        .build();
            }
        }
        log.error("Can not find position. accountId: {} symbolId: {} isLong: {}", accountId, symbolId, isLong);
        throw new OpenApiException(ApiErrorCode.ERROR_MODIFY_MARGIN);
    }

    public List<FundingRateResult> getFundingRates(Header header, String symbolId) {
        GetFundingRatesRequest request = GetFundingRatesRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .build();

        GetFundingRatesResponse response = grpcFuturesOrderService.getFundingRates(request);
        return response.getFundingInfoList().stream()
                .filter(f -> f.getLastSettleTime() > 0)
                // 过滤不属于券商的symbol资金费率
                .filter(f -> basicService.getFuturesSymbolResult(header.getOrgId(), null, f.getTokenId()) != null)
                .filter(f -> (StringUtils.isEmpty(symbolId) || f.getTokenId().equals(symbolId)))
                .map(FuturesTools::toFundingRateResult).collect(Collectors.toList());
    }

    public List<HistoryFundingRateResult> getHistoryFundingRates(Header header, String symbolId, long fromId, long endId, int limit) {
        GetHistoryFundingRatesRequest request = GetHistoryFundingRatesRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(symbolId)
                .setFromId(fromId)
                .setEndId(endId)
                .setLimit(limit)
                .build();
        List<HistoryFundingRate> rates = grpcFuturesOrderService.getHistoryFundingRates(request).getFundingInfoList();
        return rates.stream()
                .map(s -> HistoryFundingRateResult.builder()
                        .settleRate(s.getSettleRate())
                        .settleTime(s.getSettleTime())
                        .symbol(s.getTokenId())
                        .id(s.getId())
                        .build())
                .sorted(Comparator.comparing(HistoryFundingRateResult::getId).reversed())
                .collect(Collectors.toList());
    }

    public List<InsuranceFundResult> getInsuranceFunds(Header header, String symboId, Long fromId, Long endId, Integer limit) {
        GetInsuranceFundsRequest.Builder requestBuilder = GetInsuranceFundsRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setFromId(fromId)
                .setEndId(endId)
                .setLimit(limit);

        if (StringUtils.isNotEmpty(symboId)) {
            requestBuilder.setTokenId(symboId);
        }

        GetInsuranceFundsResponse response = grpcFuturesOrderService.getInsuranceFunds(requestBuilder.build());
        return response.getFundList().stream().map(FuturesTools::toInsuranceFundResult).collect(Collectors.toList());
    }

    public Map<String, FuturesAccountResult> getAccountInfo(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex) {
        GetFuturesCoinAssetRequest request = GetFuturesCoinAssetRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .build();

        GetFuturesCoinAssetResponse response = grpcFuturesOrderService.getFuturesCoinAsset(request);
        return response.getFuturesCoinAssetList().stream()
                .map(FuturesTools::toFuturesAccountResult)
                .collect(Collectors.toMap(FuturesAccountResult::getTokenId, r -> r));
    }

    public FuturesOrderResult getOrder(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                       Long orderId, String clientOrderId, String orderType) {
        ApiFuturesOrderType futuresOrderType = ApiFuturesOrderType.valueOf(orderType);
        switch (futuresOrderType) {
            case STOP:
                return getFuturesPlanOrder(header, accountId, accountType, accountIndex, orderId, clientOrderId);
            case LIMIT:
                return getFuturesOrder(header, accountId, accountType, accountIndex, orderId, clientOrderId);
            default:
                throw new OpenApiException(ApiErrorCode.INVALID_ORDER_TYPE);
        }
    }

    public BestOrderResult getBestOrder(Header header, String symbolId, Long exchangeId) {
        GetFuturesBestOrderRequest request = GetFuturesBestOrderRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setExchangeId(exchangeId)
                .setSymbolId(symbolId)
                .build();
        GetFuturesBestOrderResponse response = grpcFuturesOrderService.getFuturesBestOrder(request);
        return BestOrderResult.builder()
                .ask(getBestOrderInfo(response.getAsk()))
                .bid(getBestOrderInfo(response.getBid()))
                .price(response.getPrice())
                .build();
    }

    public DepthInfoResult getDepthInfo(Header header, Long exchangeId, String symbolIds) {
        GetDepthInfoRequest request = GetDepthInfoRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setExchangeId(exchangeId)
                .addAllSymbolId(Splitter.on(",").splitToList(symbolIds))
                .build();
        GetDepthInfoResponse response = grpcOrderService.getDepthInfo(request);
        return DepthInfoResult.builder()
                .time(response.getTime())
                .level(response.getLevel())
                .exchangeId(response.getExchangeId())
                .depthInfoList(response.getDepthInfoList().stream()
                        .map(depthInfo -> DepthInfoResult.DepthInfo.builder()
                                .symbolId(depthInfo.getSymbolId())
                                .ask(depthInfo.getAskList().stream()
                                        .map(ask -> DepthInfoResult.OrderInfoList.builder()
                                                .price(ask.getPrice())
                                                .originalPrice(ask.getOriginalPrice())
                                                .orderInfo(ask.getOrderInfoList().stream()
                                                        .map(order -> DepthInfoResult.OrderInfo.builder()
                                                                .quantity(order.getQuantity())
                                                                .accountId(order.getAccountId())
                                                                .build()).collect(Collectors.toList()))
                                                .build()).collect(Collectors.toList()))
                                .bid(depthInfo.getBidList().stream()
                                        .map(bid -> DepthInfoResult.OrderInfoList.builder()
                                                .price(bid.getPrice())
                                                .originalPrice(bid.getOriginalPrice())
                                                .orderInfo(bid.getOrderInfoList().stream()
                                                        .map(order -> DepthInfoResult.OrderInfo.builder()
                                                                .quantity(order.getQuantity())
                                                                .accountId(order.getAccountId())
                                                                .build()).collect(Collectors.toList()))
                                                .build()).collect(Collectors.toList()))
                                .build()).collect(Collectors.toList()))
                .build();
    }

    public Map<String, Object> setRiskLimit(Header header, String symbolId, Boolean isLong, Long riskLimitId,
                                            AccountTypeEnum accountType, Integer accountIndex) {
        SetRiskLimitRequest request = SetRiskLimitRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setIsLong(isLong)
                .setRiskLimitId(riskLimitId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setSymbolId(symbolId)
                .build();

        SetRiskLimitResponse response = grpcFuturesOrderService.setRiskLimit(request);
        Map<String, Object> result = new HashMap<>();
        result.put("success", response.getSuccess());
        return result;
    }

    public List<FuturesPositionResult> marketPullPositions(Header header, String symbolId, Long limit) {
        MarketPullFuturesPositionsRequest request = MarketPullFuturesPositionsRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .addSymbolIds(symbolId)
                .setLimit(limit)
                .build();
        MarketPullFuturesPositionsResponse response = grpcFuturesOrderService.marketPullFuturesPositions(request);
        return response.getPositionsList()
                .stream()
                .map(this::getMarketPullPositionResult)
                .collect(Collectors.toList());
    }

    private FuturesPositionResult getMarketPullPositionResult(FuturesPosition position) {
        FuturesPositionResult result = FuturesTools.toFuturesPositionResult(position);
        result.setAccountId(String.valueOf(position.getAccountId()));
        return result;
    }

    private BestOrderInfo getBestOrderInfo(Order order) {
        return BestOrderInfo.builder()
                .accountId(order.getAccountId())
                .price(order.getPrice())
                .orderId(order.getOrderId())
                .origQty(order.getOrigQty())
                .side(order.getOrderSide().name())
                .time(order.getTime())
                .build();
    }

    private FuturesOrderResult getFuturesOrder(Header header, Long accountId, AccountTypeEnum accountType,
                                               Integer accountIndex, Long orderId, String clientOrderId) {
        GetOrderRequest.Builder requestBuilder = GetOrderRequest.newBuilder()
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setHeader(HeaderConvertUtil.convertHeader(header));

        if (orderId != null) {
            requestBuilder.setOrderId(orderId);
        }

        if (StringUtils.isNotEmpty(clientOrderId)) {
            requestBuilder.setClientOrderId(clientOrderId);
        }

        GetOrderResponse response = grpcFuturesOrderService.getFuturesOrder(requestBuilder.build());
        return getFuturesOrderResult(response.getOrder());
    }

    private FuturesOrderResult getFuturesPlanOrder(Header header, Long accountId, AccountTypeEnum accountType,
                                                   Integer accountIndex, Long orderId, String clientOrderId) {
        GetPlanOrderRequest.Builder requestBuilder = GetPlanOrderRequest.newBuilder()
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setHeader(HeaderConvertUtil.convertHeader(header));

        if (orderId != null) {
            requestBuilder.setOrderId(orderId);
        }

        if (StringUtils.isNotEmpty(clientOrderId)) {
            requestBuilder.setClientOrderId(clientOrderId);
        }

        GetPlanOrderResponse response = grpcFuturesOrderService.getFuturesPlanOrder(requestBuilder.build());
        return getFuturesPlanOrderResult(response.getPlanOrder());
    }

    private FuturesOrderResult getFuturesOrderResult(Order order) {
        FuturesOrderResult orderResult = FuturesOrderResult.builder().build();

        List<OrderMatchFeeInfo> fees =
                order.getFeesList().stream().map(this::getOrderMatchFeeInfo).collect(Collectors.toList());

        // 订单创建时间
        orderResult.setTime(order.getTime());
        // 订单最后更新时间
        orderResult.setUpdateTime(order.getLastUpdated());
        // 订单ID
        orderResult.setOrderId(order.getOrderId());
        // 客户端下单唯一ID
        orderResult.setClientOrderId(order.getClientOrderId());
        // 期货Symbol
        orderResult.setSymbol(order.getSymbolId());
        // 委托价格
        orderResult.setPrice(order.getPrice());
        // 委托数量
        orderResult.setOrigQty(order.getOrigQty());
        // 杠杆数量
        orderResult.setLeverage(order.getLeverage());
        // 成交数量
        orderResult.setExecutedQty(order.getExecutedQty());
        orderResult.setExecuteQty(order.getExecutedQty());
        // 平均价格
        orderResult.setAvgPrice(order.getAvgPrice());
        // 保证金
        orderResult.setMarginLocked(order.getOrderMarginLocked());
        // 订单类型为LIMIT
//        orderResult.setOrderType(PlanOrder.FuturesOrderType.LIMIT.name());
        orderResult.setOrderType(order.getOrderType().name());
        // 价格类型
        orderResult.setPriceType(FuturesTools.toApiFuturesPriceType(order.getFuturesPriceType()).name());
        // 订单方向
        orderResult.setSide(order.getFuturesOrderSide().name());
        // 订单状态
        orderResult.setStatus(order.getStatusCode());
        // timeInForce
        orderResult.setTimeInForce(order.getTimeInForce().name());
        // 手续费
        orderResult.setFees(fees);

        return orderResult;
    }

    private FuturesOrderResult getFuturesPlanOrderResult(PlanOrder planOrder) {
        FuturesOrderResult orderResult = FuturesOrderResult.builder().build();

        // 计划委托下单时间
        orderResult.setTime(planOrder.getTime());
        // 计划委托订单ID
        orderResult.setOrderId(planOrder.getOrderId());
        // 订单类型: STOP
        orderResult.setOrderType(ApiFuturesOrderType.STOP.name());
        // 期货账户ID
        orderResult.setAccountId(planOrder.getAccountId());
        // 客户端下单唯一ID
        orderResult.setClientOrderId(planOrder.getClientOrderId());
        // 期货Symbol
        orderResult.setSymbol(planOrder.getSymbolId());
        // 计划委托价格
        orderResult.setPrice(planOrder.getPrice());
        // 计划委托触发价格
        orderResult.setTriggerPrice(planOrder.getTriggerPrice());
        // 计划委托数量
        orderResult.setOrigQty(planOrder.getOrigQty());
        // 杠杆数量
        orderResult.setLeverage(planOrder.getLeverage());
        // 下单方向
        orderResult.setSide(planOrder.getSide().name());
        // 计划委托单状态
        orderResult.setStatus(planOrder.getStatus().name());

        return orderResult;
    }

    private OrderMatchFeeInfo getOrderMatchFeeInfo(Fee fee) {
        return OrderMatchFeeInfo.builder()
                .feeTokenId(fee.getFeeTokenId())
                .feeTokenName(fee.getFeeTokenName())
                .fee(fee.getFee())
                .build();
    }

    private FuturesMatchResult getMatchResult(Header header, MatchInfo matchInfo) {
        BigDecimal fee = new BigDecimal(matchInfo.getFee().getFee());
        BigDecimal makerRebate = BigDecimal.ZERO;
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            makerRebate = fee.abs();
            fee = BigDecimal.ZERO;
        }
        // 期货的MatchResult只返回OPENAPI文档中规定的字段
        return FuturesMatchResult.builder()
                .orderId(matchInfo.getOrderId())
                .matchOrderId(matchInfo.getMatchOrderId())
                .tradeId(matchInfo.getTradeId())
                .symbolId(matchInfo.getSymbolId())
                .price(matchInfo.getPrice())
                .quantity(matchInfo.getQuantity())
                .feeTokenId(matchInfo.getFee().getFeeTokenId())
                .fee(fee.stripTrailingZeros().toPlainString())
                .makerRebate(makerRebate.stripTrailingZeros().toPlainString())
                .orderType(matchInfo.getOrderType().name())
                .side(FuturesTools.getFuturesSide(matchInfo.getOrderSide(), matchInfo.getIsClose()).name())
                .time(matchInfo.getTime())
                .pnl(matchInfo.getPnl())
                .build();

    }
}
