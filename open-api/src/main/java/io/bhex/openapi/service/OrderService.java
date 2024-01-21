/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.service
 *@Date 2018/6/26
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.bhex.base.account.BhTicketInfo;
import io.bhex.base.account.TicketInfo;
import io.bhex.base.proto.DecimalUtil;
import io.bhex.base.proto.OrderSideEnum;
import io.bhex.base.proto.OrderStatusEnum;
import io.bhex.base.proto.OrderTypeEnum;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.order.BatchCancelOrderRequest;
import io.bhex.broker.grpc.order.CancelOrderRequest;
import io.bhex.broker.grpc.order.CancelOrderResponse;
import io.bhex.broker.grpc.order.CreateOrderRequest;
import io.bhex.broker.grpc.order.CreateOrderResponse;
import io.bhex.broker.grpc.order.FastCancelOrderRequest;
import io.bhex.broker.grpc.order.FastCancelOrderResponse;
import io.bhex.broker.grpc.order.GetBestOrderRequest;
import io.bhex.broker.grpc.order.GetBestOrderResponse;
import io.bhex.broker.grpc.order.GetOrderMatchRequest;
import io.bhex.broker.grpc.order.GetOrderMatchResponse;
import io.bhex.broker.grpc.order.GetOrderRequest;
import io.bhex.broker.grpc.order.GetOrderResponse;
import io.bhex.broker.grpc.order.MatchInfo;
import io.bhex.broker.grpc.order.Order;
import io.bhex.broker.grpc.order.OrderQueryType;
import io.bhex.broker.grpc.order.OrderSide;
import io.bhex.broker.grpc.order.OrderTimeInForceEnum;
import io.bhex.broker.grpc.order.OrderType;
import io.bhex.broker.grpc.order.QueryAnyOrdersRequest;
import io.bhex.broker.grpc.order.QueryMatchRequest;
import io.bhex.broker.grpc.order.QueryMatchResponse;
import io.bhex.broker.grpc.order.QueryOrdersRequest;
import io.bhex.broker.grpc.order.QueryOrdersResponse;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.BestOrderInfo;
import io.bhex.openapi.domain.BestOrderResult;
import io.bhex.openapi.domain.MatchResult;
import io.bhex.openapi.domain.OrderResult;
import io.bhex.openapi.domain.SocketFuturesPositionInfo;
import io.bhex.openapi.domain.SocketOrderInfo;
import io.bhex.openapi.domain.SocketTicketInfo;
import io.bhex.openapi.domain.SymbolResult;
import io.bhex.openapi.domain.api.enums.ApiTimeInForce;
import io.bhex.openapi.domain.api.result.CancelOrderResult;
import io.bhex.openapi.domain.api.result.FastCancelOrderResult;
import io.bhex.openapi.domain.api.result.NewOrderResult;
import io.bhex.openapi.domain.api.result.QueryOrderResult;
import io.bhex.openapi.domain.api.result.TradeResult;
import io.bhex.openapi.grpc.client.GrpcMarginService;
import io.bhex.openapi.grpc.client.GrpcOrderService;
import io.bhex.openapi.util.DecimalFormatter;
import io.bhex.openapi.util.FuturesTools;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderService {

    @Resource
    private GrpcOrderService grpcOrderService;

    @Resource
    private BasicService basicService;
    @Resource
    private GrpcMarginService grpcMarginService;

    public NewOrderResult newOrder(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                   Long exchangeId, String symbolId, String clientOrderId,
                                   String orderSide, String orderType, String price, String quantity, ApiTimeInForce timeInForce,
                                   String methodVersion, String orderSource) {
        CreateOrderRequest request = CreateOrderRequest.newBuilder()
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
                .setTimeInForce(OrderTimeInForceEnum.valueOf(timeInForce.name()))
                .setOrderSource(orderSource)
                .build();
//        CreateOrderResponse response = null;
//        if (!Strings.isNullOrEmpty(methodVersion) && methodVersion.equals("2.0")) {
//            response = grpcOrderService.createOrderV20(HeaderConvertUtil.convertHeader(header), request);
//        } else if (!Strings.isNullOrEmpty(methodVersion) && methodVersion.equals("2.1")) {
//            response = grpcOrderService.createOrderV21(HeaderConvertUtil.convertHeader(header), request);
//        } else {
//            response = grpcOrderService.createOrder(HeaderConvertUtil.convertHeader(header), request);
//        }
        CreateOrderResponse response;
        if (accountType == AccountTypeEnum.MARGIN) {
            response = grpcMarginService.marginCreateOrder(HeaderConvertUtil.convertHeader(header), request);
        } else {
            response = grpcOrderService.createOrder(HeaderConvertUtil.convertHeader(header), request);
        }

        if (response.getOrder().getStatusCode().equals(OrderStatusEnum.REJECTED.name())) {
            throw new OpenApiException(ApiErrorCode.NEW_ORDER_REJECTED);
        }
        return NewOrderResult.convertToResult(response.getOrder());
    }

    public CancelOrderResult cancelOrder(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                         String clientOrderId, Long orderId, String methodVersion) {
        if (Strings.isNullOrEmpty(clientOrderId) && orderId == 0) {
            throw new BrokerException(BrokerErrorCode.REQUEST_INVALID);
        }
        CancelOrderRequest request = CancelOrderRequest.newBuilder()
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setOrderId(orderId)
                .setClientOrderId(clientOrderId)
                .build();
        CancelOrderResponse response = grpcOrderService.cancelOrder(HeaderConvertUtil.convertHeader(header), request);
        Order order = response.getOrder();
        return CancelOrderResult.builder()
                .accountId(order.getAccountId())
                .symbol(order.getSymbolId())
                .clientOrderId(order.getClientOrderId())
                .orderId(order.getOrderId())
                .transactTime(order.getTime())
                .price(order.getPrice())
                .origQty(order.getOrigQty())
                .executedQty(order.getExecutedQty())
                .status(order.getStatusCode())
                .timeInForce(order.getTimeInForce().name())
                .type(order.getOrderType().name())
                .side(order.getOrderSide().name())
                .build();
    }


    public FastCancelOrderResult fastCancelOrder(Header header, Long accountId, String clientOrderId, Long orderId, String symbolId, Integer securityType, AccountTypeEnum accountType, Integer accountIndex) {
        if (Strings.isNullOrEmpty(clientOrderId) && orderId <= 0) {
            throw new BrokerException(BrokerErrorCode.REQUEST_INVALID);
        }

        if (StringUtils.isEmpty(symbolId)) {
            throw new BrokerException(BrokerErrorCode.INVALID_SYMBOL);
        }
        FastCancelOrderRequest request = FastCancelOrderRequest.newBuilder()
                .setAccountId(accountId)
                .setOrderId(orderId == null ? 0L : orderId)
                .setClientOrderId(Strings.isNullOrEmpty(clientOrderId) ? "" : clientOrderId)
                .setSymbolId(symbolId)
                .setSecurityType(securityType)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .build();
        FastCancelOrderResponse response = grpcOrderService.fastCancelOrder(HeaderConvertUtil.convertHeader(header), request);
        return FastCancelOrderResult.builder().isCancelled(response.getIsCancelled()).build();
    }

    public void batchCancelOrder(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                 String symbolId, String orderSide) {
        BatchCancelOrderRequest.Builder builder = BatchCancelOrderRequest.newBuilder()
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .addSymbolIds(Strings.nullToEmpty(symbolId));
        if (!Strings.isNullOrEmpty(orderSide)) {
            builder.setOrderSide(OrderSide.valueOf(orderSide.toUpperCase()));
        }
        grpcOrderService.batchCancel(HeaderConvertUtil.convertHeader(header), builder.build());
    }

    public QueryOrderResult getOrder(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                     String clientOrderId, Long orderId) {
        if (Strings.isNullOrEmpty(clientOrderId) && orderId == 0) {
            throw new OpenApiException(ApiErrorCode.UNKNOWN_PARAM);
        }
        GetOrderRequest request = GetOrderRequest.newBuilder()
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setOrderId(orderId)
                .setClientOrderId(clientOrderId)
                .build();
        GetOrderResponse response = grpcOrderService.getOrder(HeaderConvertUtil.convertHeader(header), request);
        return QueryOrderResult.convert(response.getOrder());
    }

    public List<QueryOrderResult> queryOrders(Header header, OrderQueryType queryType, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                              String symbolId, String side, Long fromOrderId, Long endOrderId, Long startTime, Long endTime, Integer limit) {
        QueryOrdersRequest request = QueryOrdersRequest.newBuilder()
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setSymbolId(symbolId)
                .setFromId(fromOrderId)
                .setEndId(endOrderId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLimit(limit)
                .setQueryType(queryType)
                .build();
        if (!Strings.isNullOrEmpty(side) && (side.equalsIgnoreCase("sell") || side.equalsIgnoreCase("buy"))) {
            request = request.toBuilder().setOrderSide(OrderSide.valueOf(side.toUpperCase())).build();
        }
        QueryOrdersResponse response = grpcOrderService.queryOrders(HeaderConvertUtil.convertHeader(header), request);
        return Lists.newArrayList(response.getOrdersList()).stream().map(QueryOrderResult::convert).collect(Collectors.toList());
    }

    public List<QueryOrderResult> queryAnyOrders(Header header, OrderQueryType queryType, Long accountId,
                                                 String symbolId, Long fromOrderId, Long endOrderId, Long startTime, Long endTime, Integer limit) {
        QueryAnyOrdersRequest request = QueryAnyOrdersRequest.newBuilder()
                .setAnyAccountId(accountId)
                .setSymbolId(symbolId)
                .setFromId(fromOrderId)
                .setEndId(endOrderId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLimit(limit)
                .setQueryType(queryType)
                .build();
        QueryOrdersResponse response = grpcOrderService.queryAnyOrders(HeaderConvertUtil.convertHeader(header), request);
        return Lists.newArrayList(response.getOrdersList()).stream().map(QueryOrderResult::convert).collect(Collectors.toList());
    }

    private OrderResult getOrderResult(Order order) {
        return OrderResult.builder()
                .accountId(order.getAccountId())
                .orderId(order.getOrderId())
                .clientOrderId(order.getClientOrderId())
                .symbol(order.getSymbolId())
                .price(order.getPrice())
                .origQty(order.getOrigQty())
                .executedQty(order.getExecutedQty())
                .executedAmount(order.getExecutedAmount())
                .avgPrice(order.getAvgPrice())
                .type(order.getOrderType().name())
                .side(order.getOrderSide().name())
                .status(order.getStatusCode())
                .time(order.getTime())
                .build();
    }

    private BestOrderInfo getBestOrderInfo(Order order) {
        return BestOrderInfo.builder()
                .accountId(order.getAccountId())
                .orderId(order.getOrderId())
                .price(order.getPrice())
                .origQty(order.getOrigQty())
                .type(order.getOrderType().name())
                .side(order.getOrderSide().name())
                .status(order.getStatusCode())
                .time(order.getTime())
                .build();
    }

    public List<SocketOrderInfo> getSocketOrderInfo(io.bhex.base.account.Order order) {
        if (order.getTicketInfosList().size() > 0) {
            return order.getTicketInfosList().stream()
                    .map(ticketInfo -> getSocketOrderInfo(order, ticketInfo))
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList(getSocketOrderInfo(order, null));
        }
    }

    private SocketOrderInfo getSocketOrderInfo(io.bhex.base.account.Order order, TicketInfo ticketInfo) {
        long matchAccountId = 0L;
        long matchOrderId = 0L;
        if (ticketInfo != null) {
            matchOrderId = ticketInfo.getMatchOrderId();
            if (order.getAccountType() == 24) {
                if (basicService.isUserInFuturesWhiteList(order.getBrokerUserId())) {
                    log.info("userId: {} in FuturesUserWhiteList.", order.getBrokerUserId());
                    matchAccountId = ticketInfo.getMatchAccountId();
                }
            } else if (order.getAccountType() == 1) {
                if (basicService.isUserInCoinWhiteList(order.getBrokerUserId())) {
                    log.info("userId: {} in CoinUserWhiteList.", order.getBrokerUserId());
                    matchAccountId = ticketInfo.getMatchAccountId();
                }
            }
        }

        BigDecimal orderPrice = DecimalUtil.toBigDecimal(order.getPrice());

        String orderSideVal;
        String orderPriceVal;
        String lastExecutedPriceVal;

        if (order.getAccountType() == 24) {
            SymbolResult symbolResult = basicService.getFuturesSymbolResult(
                    order.getOrgId(), null, order.getSymbolId());
            if (symbolResult == null) {
                log.warn("Get SymbolResult by orgId: {} symbolId: {} error", order.getOrgId(), order.getSymbolId());
                throw new BrokerException(BrokerErrorCode.SYSTEM_ERROR);
            }

            DecimalFormatter priceFmt = new DecimalFormatter(new BigDecimal(symbolResult.getMinPricePrecision()));

            orderPriceVal = priceFmt.format(orderPrice);
            orderSideVal = order.getSide().name();
            lastExecutedPriceVal = ticketInfo == null ? "" : priceFmt.format(ticketInfo.getPrice());

            // 根据合约的正反向设置买卖方向和价格
            if (symbolResult.getIsReverse()) {
                if (order.getSide() == OrderSideEnum.BUY) {
                    orderSideVal = OrderSideEnum.SELL.name();
                } else {
                    orderSideVal = OrderSideEnum.BUY.name();
                }

                orderPriceVal = priceFmt.reciprocalFormat(orderPrice);
                lastExecutedPriceVal = ticketInfo == null ? "" : priceFmt.reciprocalFormat(ticketInfo.getPrice());
            }

        } else {
            orderSideVal = order.getSide().name();
            orderPriceVal = orderPrice.stripTrailingZeros().toPlainString();
            lastExecutedPriceVal = ticketInfo == null ? "0" : DecimalUtil.toBigDecimal(ticketInfo.getPrice()).stripTrailingZeros().toPlainString();
        }

        return SocketOrderInfo.builder()
                .eventType("executionReport")
                .eventTime(System.currentTimeMillis())
                .symbol(order.getSymbol().getSymbolId())
                .clientOrderId(order.getClientOrderId())
                .orderSide(orderSideVal)
                .orderType(order.getType().name())
                .timeInForce(order.getTimeInForce().name())
                .quantity(order.getType() == OrderTypeEnum.MARKET_OF_QUOTE
                        ? DecimalUtil.toBigDecimal(order.getAmount()).stripTrailingZeros().toPlainString()
                        : DecimalUtil.toBigDecimal(order.getQuantity()).stripTrailingZeros().toPlainString())
                .price(orderPriceVal)
                .status(order.getStatus().name())
                .orderId(order.getOrderId())
                .matchOrderId(matchOrderId)
                .executedQuantity(DecimalUtil.toBigDecimal(order.getExecutedQuantity()).stripTrailingZeros().toPlainString())
                .isWorking(true)
                .isMaker(order.getType() == OrderTypeEnum.LIMIT_MAKER)
                .createTime(order.getCreatedTime())
                .executedAmount(DecimalUtil.toBigDecimal(order.getExecutedAmount()).stripTrailingZeros().toPlainString())
                .lastExecutedQuantity(ticketInfo == null ? "0" : DecimalUtil.toBigDecimal(ticketInfo.getQuantity()).stripTrailingZeros().toPlainString())
                .lastExecutedPrice(lastExecutedPriceVal)
                .commissionAmount(ticketInfo == null ? "0" : DecimalUtil.toBigDecimal(ticketInfo.getTradeFee().getFee()).stripTrailingZeros().toPlainString())
                .commissionAsset(ticketInfo == null ? "" : ticketInfo.getTradeFee().getToken().getTokenId())
                .isNormal(ticketInfo == null ? Boolean.TRUE : ticketInfo.getIsNormal())
                .matchAccountId(matchAccountId)
                .isClose(order.getIsClose())
                .leverage(DecimalUtil.toBigDecimal(order.getLeverage()).stripTrailingZeros().toPlainString())
                .build();
    }

    public List<TradeResult> queryMatchInfo(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                            String symbolId, Long fromTraderId, Long endTradeId,
                                            Long startTime, Long endTime, Integer limit, String withIsNormal) {
        QueryMatchRequest request = QueryMatchRequest.newBuilder()
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .setSymbolId(Strings.nullToEmpty(symbolId))
                .setFromId(fromTraderId)
                .setEndId(endTradeId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLimit(limit)
                .build();
        QueryMatchResponse response = grpcOrderService.queryMatchInfo(HeaderConvertUtil.convertHeader(header), request);
        return Lists.newArrayList(response.getMatchList()).stream().map(info -> {
            TradeResult tradeResult = TradeResult.convert(info);
            if (withIsNormal != null && withIsNormal.equals("1")) {
                tradeResult.setIsNormal(info.getIsNormal());
            }
            return tradeResult;
        }).collect(Collectors.toList());
    }

    public List<MatchResult> getOrderMatchInfo(Header header, Long accountId, Long orderId, Long fromTraderId, Integer limit) {
        GetOrderMatchRequest request = GetOrderMatchRequest.newBuilder()
                .setAccountId(accountId)
                .setOrderId(orderId)
                .setFromId(fromTraderId)
                .setLimit(limit)
                .build();
        GetOrderMatchResponse response = grpcOrderService.getOrderMatchInfo(HeaderConvertUtil.convertHeader(header), request);
        return Lists.newArrayList(response.getMatchList()).stream().map(this::getMatchResult).collect(Collectors.toList());
    }

    private MatchResult getMatchResult(MatchInfo matchInfo) {
        BigDecimal fee = new BigDecimal(matchInfo.getFee().getFee());
        BigDecimal makerRebate = BigDecimal.ZERO;
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            makerRebate = fee.abs();
            fee = BigDecimal.ZERO;
        }
        return MatchResult.builder()
                .accountId(matchInfo.getAccountId())
                .orderId(matchInfo.getOrderId())
                .tradeId(matchInfo.getTradeId())
                .symbolId(matchInfo.getSymbolId())
                .symbolName(matchInfo.getSymbolName())
                .baseTokenId(matchInfo.getBaseTokenId())
                .baseTokenName(matchInfo.getBaseTokenName())
                .quoteTokenId(matchInfo.getQuoteTokenId())
                .quoteTokenName(matchInfo.getQuoteTokenName())
                .price(matchInfo.getPrice())
                .quantity(matchInfo.getQuantity())
                .feeTokenId(matchInfo.getFee().getFeeTokenId())
                .feeTokenName(matchInfo.getFee().getFeeTokenName())
                .fee(fee.stripTrailingZeros().toPlainString())
                .makerRebate(makerRebate.stripTrailingZeros().toPlainString())
                .side(matchInfo.getOrderSide().name())
                .type(matchInfo.getOrderType().name())
                .time(matchInfo.getTime())
                .build();
    }

    public BestOrderResult getBestOrder(Header header, Long exchangeId, String symbolId) {
        GetBestOrderRequest request = GetBestOrderRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setExchangeId(exchangeId)
                .setSymbolId(symbolId)
                .build();
        GetBestOrderResponse response = grpcOrderService.getBestOrder(request);
        return BestOrderResult.builder()
                .price(response.getPrice())
                .bid(getBestOrderInfo(response.getBid()))
                .ask(getBestOrderInfo(response.getAsk()))
                .build();
    }

    public SocketFuturesPositionInfo getSocketFuturesPositionInfo(io.bhex.base.account.FuturesPosition position) {
        SymbolResult symbolResult = basicService.getFuturesSymbolResult(position.getOrgId(), null, position.getTokenId());
        if (symbolResult == null) {
            log.warn("getSocketFuturesPositionInfo get SymbolResult null. orgId: {} symbolId: {}",
                    position.getOrgId(), position.getTokenId());
            throw new BrokerException(BrokerErrorCode.SYSTEM_ERROR);
        }
        return FuturesTools.toSocketFuturesPositionInfo(position, symbolResult);
    }

    public SocketTicketInfo getSocketTicketInfo(BhTicketInfo bhTicketInfo) {
        return SocketTicketInfo.builder()
                .eventType("ticketInfo")
                .eventTime(System.currentTimeMillis())
                .time(bhTicketInfo.getMatchTime())
                .symbol(bhTicketInfo.getSymbolId())
                .price(DecimalUtil.toTrimString(bhTicketInfo.getPrice()))
                .quantity(DecimalUtil.toTrimString(bhTicketInfo.getQuantity()))
                .orderId(bhTicketInfo.getOrderId())
                .ticketId(bhTicketInfo.getTicketId())
                .clientOrderId(bhTicketInfo.getClientOrderId())
                .matchOrderId(bhTicketInfo.getMatchOrderId())
                .accountId(bhTicketInfo.getAccountId())
                .matchAccountId(bhTicketInfo.getMatchAccountId())
                .build();
    }
}
