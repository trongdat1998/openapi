/*
 ************************************
 * @项目名称: api-parent
 * @文件名称: OptionOrderService
 * @Date 2019/01/09
 * @Author will.zhao@bhex.io
 * @Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 **************************************
 */
package io.bhex.openapi.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.protobuf.TextFormat;
import io.bhex.base.proto.OrderStatusEnum;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.order.OrderSide;
import io.bhex.broker.grpc.order.OrderType;
import io.bhex.broker.grpc.order.*;
import io.bhex.openapi.domain.*;
import io.bhex.openapi.grpc.client.GrpcOptionOrderService;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OptionOrderService {

    @Resource
    GrpcOptionOrderService grpcOptionOrderService;

    @Resource
    private MessageSource messageSource;

    public OrderResult newOptionOrder(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                      Long exchangeId, String symbolId, String clientOrderId,
                                      String orderSide, String orderType, String price, String quantity, String timeInForce, String orderSource) {
        if (Strings.isNullOrEmpty(orderSide) || Strings.isNullOrEmpty(orderType)) {
            throw new BrokerException(BrokerErrorCode.PARAM_INVALID);
        }
        CreateOrderRequest request = CreateOrderRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setExchangeId(exchangeId)
                .setSymbolId(symbolId)
                .setClientOrderId(clientOrderId)
                .setOrderSide(OrderSide.valueOf(orderSide.toUpperCase()))
                .setOrderType(OrderType.valueOf(orderType.toUpperCase()))
                .setPrice(price)
                .setQuantity(quantity)
                .setTimeInForce(OrderTimeInForceEnum.valueOf(timeInForce))
                .setOrderSource(orderSource)
                .build();
        CreateOrderResponse response = grpcOptionOrderService.createOptionOrder(request);
        Order order = response.getOrder();
        if (order.getStatusCode().equals(OrderStatusEnum.REJECTED.name())) {
            log.warn("newOrder: REJECTED (request: {})", TextFormat.shortDebugString(request));
            throw new BrokerException(BrokerErrorCode.ORDER_FAILED, new RuntimeException("match rejected"));
        }
        return getOptionOrderResult(response.getOrder());
    }

    public OrderResult cancelOptionOrder(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex, String clientOrderId, Long orderId) {
        if ((Strings.isNullOrEmpty(clientOrderId) || clientOrderId.equals("0")) && (orderId == null || orderId == 0)) {
            throw new BrokerException(BrokerErrorCode.REQUEST_INVALID);
        }
        CancelOrderRequest request = CancelOrderRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setOrderId(orderId)
                .setClientOrderId(clientOrderId)
                .build();
        CancelOrderResponse response = grpcOptionOrderService.cancelOptionOrder(request);
        Order order = response.getOrder();
        if (order != null) {
            return getOptionOrderResult(order);
        }
        return getOptionOrder(header, accountId, clientOrderId, orderId);
    }

    public void batchCancelOptionOrder(Header header, Long accountId, String symbolIds, String orderSide) {
        BatchCancelOrderRequest.Builder builder = BatchCancelOrderRequest.newBuilder();
        List<String> symbolIdList = Optional.of(Lists.newArrayList(symbolIds.split(",")))
                .orElse(new ArrayList<>());
        builder.addAllSymbolIds(symbolIdList);
        if (!Strings.isNullOrEmpty(orderSide)) {
            builder.setOrderSide(OrderSide.valueOf(orderSide.toUpperCase()));
        }
        builder.setHeader(HeaderConvertUtil.convertHeader(header)).setAccountId(accountId);
        BatchCancelOrderResponse response = grpcOptionOrderService.batchCancelOptionOrder(builder.build());
    }

    public OrderResult getOptionOrder(Header header, Long accountId, String clientOrderId, Long orderId) {
        if ((Strings.isNullOrEmpty(clientOrderId) || clientOrderId.equals("0")) && (orderId == null || orderId == 0)) {
            throw new BrokerException(BrokerErrorCode.REQUEST_INVALID);
        }
        GetOrderRequest request = GetOrderRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setOrderId((orderId != null && orderId > 0l) ? orderId : 0l)
                .setClientOrderId(StringUtils.isNotEmpty(clientOrderId) ? clientOrderId : "")
                .build();
        GetOrderResponse response = grpcOptionOrderService.getOptionOrder(request);
        return getOptionOrderResult(response.getOrder());
    }

    private OrderResult getOptionOrderResult(Order order) {
        String statusCode = order.getStatusCode();
        List<OrderMatchFeeInfo> fees = order.getFeesList().stream()
                .map(fee -> OrderMatchFeeInfo.builder()
                        .feeTokenId(fee.getFeeTokenId())
                        .feeTokenName(fee.getFeeTokenName())
                        .fee(fee.getFee())
                        .build())
                .collect(Collectors.toList());
        return OrderResult.builder()
                .orderId(order.getOrderId())
                .clientOrderId(order.getClientOrderId())
                .symbol(order.getSymbolId())
                .price(order.getPrice())
                .origQty(order.getOrigQty())
                .executedQty(order.getExecutedQty())
                .avgPrice(order.getAvgPrice())
                .type(order.getOrderType() == OrderType.MARKET ? OrderType.MARKET.name() : OrderType.LIMIT.name())
                .side(order.getOrderSide().name())
                .status(statusCode)
                .time(order.getTime())
                .timeInForce(order.getTimeInForce().name())
                .updateTime(order.getLastUpdated())
                .fees(fees)
                .build();
    }

    /**
     * 获取用户期权委托
     *
     * @param header       header
     * @param accountId    accountId
     * @param symbolId     symbolId
     * @param fromOrderId  fromOrderId
     * @param endOrderId   endOrderId
     * @param startTime    startTime
     * @param endTime      endTime
     * @param baseTokenId  baseTokenId
     * @param quoteTokenId quoteTokenId
     * @param orderType    orderType
     * @param orderSide    orderSide
     * @param limit        limit
     * @return list
     */
    public List<OrderResult> queryCurrentOrders(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                                String symbolId, Long fromOrderId, Long endOrderId, Long startTime, Long endTime,
                                                String baseTokenId, String quoteTokenId, String orderType, String orderSide,
                                                Integer limit) {
        QueryOrdersRequest.Builder builder = QueryOrdersRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setSymbolId(symbolId)
                .setFromId(fromOrderId)
                .setEndId(endOrderId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLimit(limit)
                .setBaseTokenId(baseTokenId)
                .setQuoteTokenId(quoteTokenId)
                .setQueryType(OrderQueryType.CURRENT);
        if (!Strings.isNullOrEmpty(orderType)) {
            builder.setOrderType(OrderType.valueOf(orderType.toUpperCase()));
        }
        if (!Strings.isNullOrEmpty(orderSide)) {
            builder.setOrderSide(OrderSide.valueOf(orderSide.toUpperCase()));
        }
        QueryOrdersResponse response = grpcOptionOrderService.queryOptionOrders(builder.build());
        return Lists.newArrayList(response.getOrdersList()).stream().map(this::getOptionOrderResult).collect(Collectors.toList());
    }


    /**
     * 期权历史委托
     *
     * @param header       header
     * @param accountId    accountId
     * @param symbolId     symbolId
     * @param fromOrderId  fromOrderId
     * @param endOrderId   endOrderId
     * @param startTime    startTime
     * @param endTime      endTime
     * @param baseTokenId  baseTokenId
     * @param quoteTokenId quoteTokenId
     * @param orderType    orderType
     * @param orderSide    orderSide
     * @param limit        limit
     * @param orderStatus  orderStatus
     * @return list
     */
    public List<OrderResult> queryHistoryOrders(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                                String symbolId, Long fromOrderId, Long endOrderId, Long startTime, Long endTime,
                                                String baseTokenId, String quoteTokenId, String orderType, String orderSide,
                                                Integer limit, String orderStatus) {
        QueryOrdersRequest.Builder builder = QueryOrdersRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setSymbolId(symbolId)
                .setFromId(fromOrderId)
                .setEndId(endOrderId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setBaseTokenId(baseTokenId)
                .setQuoteTokenId(quoteTokenId)
                .setLimit(limit)
                .setQueryType(OrderQueryType.HISTORY);
        if (!Strings.isNullOrEmpty(orderType)) {
            builder.setOrderType(OrderType.valueOf(orderType.toUpperCase()));
        }
        if (!Strings.isNullOrEmpty(orderSide)) {
            builder.setOrderSide(OrderSide.valueOf(orderSide.toUpperCase()));
        }
        if (!Strings.isNullOrEmpty(orderStatus)) {
            builder.addAllOrderStatus(Arrays.asList(OrderStatus.valueOf(orderStatus.toUpperCase())));
        } else {
            builder.addAllOrderStatus(Arrays.asList(OrderStatus.CANCELED, OrderStatus.FILLED));
        }
        QueryOrdersResponse response = grpcOptionOrderService.queryOptionOrders(builder.build());
        return Lists.newArrayList(response.getOrdersList()).stream().map(this::getOptionOrderResult).collect(Collectors.toList());
    }

    /**
     * 期权历史成交
     *
     * @param header       header
     * @param accountId    accountId
     * @param symbolId     symbolId
     * @param fromTraderId fromTraderId
     * @param endTradeId   endTradeId
     * @param startTime    startTime
     * @param endTime      endTime
     * @param limit        limit
     * @return list
     */
    public List<MatchResult> queryMatchInfo(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                            String symbolId, Long fromTraderId, Long endTradeId,
                                            Long startTime, Long endTime, Integer limit, String side) {

        QueryMatchRequest request = QueryMatchRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setSymbolId(symbolId)
                .setFromId(fromTraderId)
                .setEndId(endTradeId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLimit(limit)
                .setOrderSide(StringUtils.isNotEmpty(side) ? OrderSide.valueOf(side.toUpperCase()) : OrderSide.UNKNOWN_ORDER_SIDE)
                .build();
        QueryMatchResponse response = grpcOptionOrderService.queryOptionMatchInfo(request);
        return Lists.newArrayList(response.getMatchList()).stream().map(this::getMatchResult).collect(Collectors.toList());
    }

    /**
     * 成交历史detail list
     *
     * @param header       header
     * @param accountId    accountId
     * @param orderId      orderId
     * @param fromTraderId fromTraderId
     * @param limit        limit
     * @return list
     */
    public List<MatchResult> getOrderMatchInfo(Header header, Long accountId, Long orderId, Long fromTraderId, Integer limit) {
        GetOrderMatchRequest request = GetOrderMatchRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setOrderId(orderId)
                .setFromId(fromTraderId)
                .setLimit(limit)
                .build();
        GetOrderMatchResponse response = grpcOptionOrderService.getOptionOrderMatchInfo(request);
        return Lists.newArrayList(response.getMatchList()).stream().map(this::getMatchResult).collect(Collectors.toList());
    }

    /**
     * 获取持仓数据
     *
     * @param header   header
     * @param tokenIds tokenIds
     * @return list
     */
    public List<PositionResult> getOptionPositions(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                                   String tokenIds, Integer exchangeId,
                                                   Long fromBalanceId, Long endBalanceId, Integer limit) {
        OptionPositionsRequest positionsRequest = OptionPositionsRequest
                .newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setTokenIds(tokenIds != null ? tokenIds : "")
                .setExchangeId(exchangeId != null ? exchangeId : 0)
                .setFromBalanceId(fromBalanceId != null ? fromBalanceId : 0)
                .setEndBalanceId(endBalanceId != null ? endBalanceId : 0)
                .setLimit(limit)
                .build();
        OptionPositionsResponse response = grpcOptionOrderService.getOptionPositions(positionsRequest);
        List<PositionResult> positionResults = new ArrayList<>();
        response.getOptionPositionList().forEach(s -> positionResults.add(buildPositionResult(s)));
        return positionResults;
    }

    /**
     * 构建positionResult
     *
     * @param optionPositions optionPositions
     * @return positionResult
     */
    private PositionResult buildPositionResult(OptionPosition optionPositions) {
        return PositionResult
                .builder()
                .settlementTime(optionPositions.getSettlementTime())
                .strikePrice(optionPositions.getStrikePrice())
                .margin(optionPositions.getMargin())
                .position(optionPositions.getTotal())
                .averagePrice(optionPositions.getAveragePrice())
                .changedRate(optionPositions.getChangedRate())
                .price(optionPositions.getPrice())
                .availablePosition(optionPositions.getAvailPosition())
                .symbol(optionPositions.getSymbolId())
                .changed(optionPositions.getChanged())
                .index(StringUtils.isNotEmpty(optionPositions.getIndices())
                        ? new BigDecimal(optionPositions.getIndices()).toPlainString()
                        : "0.00")
                .build();
    }

    /**
     * 获取交割数据
     *
     * @param header           header
     * @param side             side
     * @param fromSettlementId fromSettlementId
     * @param endSettlementId  endSettlementId
     * @param startTime        startTime
     * @param endTime          endTime
     * @param limit            limit
     * @return list
     */
    public List<SettlementResult> getOptionSettlement(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                                      String side, Long fromSettlementId, Long endSettlementId,
                                                      Long startTime, Long endTime, Integer limit) {
        OptionSettlementRequest settlementRequest = OptionSettlementRequest
                .newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setSide(StringUtils.isNotEmpty(side) ? side : "")
                .setFromSettlementId(fromSettlementId != null ? fromSettlementId : 0L)
                .setEndSettlementId(endSettlementId != null ? endSettlementId : 0L)
                .setStartTime(startTime != null ? startTime : 0L)
                .setEndTime(endTime != null ? endTime : 0L)
                .setLimit(limit)
                .build();
        OptionSettlementResponse response = grpcOptionOrderService.getOptionSettlement(settlementRequest);
        List<SettlementResult> settlementResults = new ArrayList<>();
        response.getOptionSettlementList().forEach(s -> settlementResults.add(buildSettlementResult(s)));
        return settlementResults;
    }

    /**
     * 构造交割数据
     *
     * @param optionSettlements 参数
     * @return SettlementResult
     */
    private SettlementResult buildSettlementResult(OptionSettlement optionSettlements) {
        return SettlementResult
                .builder()
                .symbol(optionSettlements.getSymbolId())
                .position(optionSettlements.getAvailable())
                .averagePrice(optionSettlements.getAveragePrice())
                .changed(optionSettlements.getChanged())
                .changedRate(optionSettlements.getChangedRate())
                .costPrice(optionSettlements.getCostPrice())
                .margin(optionSettlements.getMargin())
                .maxPayOff(optionSettlements.getMaxPayOff())
                .timestamp(optionSettlements.getSettlementTime())
                .settlementPrice(optionSettlements.getSettlementPrice())
                .strikePrice(optionSettlements.getStrikePrice())
                .changed(optionSettlements.getChanged())
                .costPrice(optionSettlements.getCostPrice())
                .optionType(getOptionType(optionSettlements.getSymbolId()))
                .build();
    }

    private MatchResult getMatchResult(MatchInfo matchInfo) {
        String symbolName = messageSource.getMessage(matchInfo.getSymbolId(),
                null, "", LocaleContextHolder.getLocale());
        return MatchResult.builder()
                .orderId(matchInfo.getOrderId())
                .matchOrderId(matchInfo.getMatchOrderId())
                .tradeId(matchInfo.getTradeId())
                .price(matchInfo.getPrice())
                .quantity(matchInfo.getQuantity())
                .feeTokenId(matchInfo.getFee().getFeeTokenId())
                .feeTokenName(matchInfo.getFee().getFeeTokenName())
                .fee(matchInfo.getFee().getFee())
                .makerRebate(BigDecimal.ZERO.stripTrailingZeros().toPlainString())
                .side(matchInfo.getOrderSide().name())
                .type(matchInfo.getOrderType() == OrderType.MARKET ? OrderType.MARKET.name() : OrderType.LIMIT.name())
                .time(matchInfo.getTime())
                .symbol(matchInfo.getSymbolId())
                .build();
    }

    private String getOptionType(String symbolId) {
        if (symbolId.toUpperCase().contains("CALL")) {
            return "call";
        } else {
            return "put";
        }
    }
}
