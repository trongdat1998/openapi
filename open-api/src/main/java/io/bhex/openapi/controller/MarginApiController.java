package io.bhex.openapi.controller;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.bhex.base.log.LogBizConstants;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.broker.core.validate.HbtcParamType;
import io.bhex.broker.core.validate.ValidHbtcParam;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.margin.*;
import io.bhex.broker.grpc.order.OrderQueryType;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.AccountType;
import io.bhex.openapi.domain.BalanceFlowResult;
import io.bhex.openapi.domain.SymbolResult;
import io.bhex.openapi.domain.api.enums.ApiOrderSide;
import io.bhex.openapi.domain.api.enums.ApiOrderType;
import io.bhex.openapi.domain.api.enums.ApiTimeInForce;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.domain.api.result.*;
import io.bhex.openapi.interceptor.annotation.LimitAuth;
import io.bhex.openapi.interceptor.annotation.SignAuth;
import io.bhex.openapi.service.AccountService;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.service.MarginService;
import io.bhex.openapi.service.OrderService;
import io.bhex.openapi.util.ReadOnlyApiKeyCheckUtil;
import io.bhex.openapi.util.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-04 18:39
 */
@Slf4j
@Validated
@RestController
@RequestMapping(value = {"/openapi/v1/margin"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class MarginApiController {

    private static final Integer MAX_QUERY_ORDERS_LIMIT = 500;
    private static final Integer DEFAULT_QUERY_TRADES_LIMIT = 500;
    private static final Integer MAX_QUERY_TRADES_LIMIT = 1000;

    private static final String PARAM_SYMBOL_NAME = "symbol";

    private static final String PARAM_QUANTITY_NAME = "quantity";

    @Autowired
    private OrderService orderService;

    @Autowired
    private BasicService basicService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private MarginService marginService;

    /**
     * 杠杆下单
     *
     * @param header
     * @param symbol
     * @param side
     * @param type
     * @param quantity
     * @param price
     * @param timeInForce
     * @param newClientOrderId
     * @param recvWindow
     * @param timestamp
     * @param methodVersion
     * @param orderSource
     * @param request
     * @return
     */
    @SignAuth(checkRecvWindow = true, requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT, RateLimitType.ORDERS}, weight = 1)
    @PostMapping(value = "/order")
    public String newMarginOrder(Header header,
                                 @RequestParam(name = "symbol", required = true) String symbol,
                                 @RequestParam(name = "side", required = true) String side,
                                 @RequestParam(name = "type", required = true) String type,
                                 @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_QUANTITY)
                                 @RequestParam(name = "quantity", required = true) BigDecimal quantity,
                                 @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_PRICE)
                                 @RequestParam(name = "price", required = false, defaultValue = "0") BigDecimal price,
                                 @RequestParam(name = "timeInForce", required = false, defaultValue = "GTC") String timeInForce,
                                 @ValidHbtcParam(message = "30003", type = HbtcParamType.CLIENT_ORDER_ID)
                                 @RequestParam(name = "newClientOrderId", required = false, defaultValue = "") String newClientOrderId,
                                 @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                 @RequestParam(name = "timestamp", required = true) Long timestamp,
                                 @RequestParam(name = "methodVersion", required = false, defaultValue = "") String methodVersion,
                                 @RequestParam(name = "orderSource", required = false, defaultValue = "") String orderSource,
                                 HttpServletRequest request) {
        MDC.put(LogBizConstants.ORDER_ID, newClientOrderId);

        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);

        SymbolResult symbolResult = basicService.querySymbol(header, symbol);
        if (symbolResult == null || !symbolResult.getAllowMargin()) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, PARAM_SYMBOL_NAME);
        }
        Long exchangeId = symbolResult.getExchangeId();

        ApiOrderSide apiOrderSide = ApiOrderSide.fromValue(side.toLowerCase());
        if (apiOrderSide == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_SIDE);
        }

        ApiOrderType apiOrderType = ApiOrderType.fromValue(type);
        if (apiOrderType == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_ORDER_TYPE);
        }

        // timeInForce 只支持GTC
        if (StringUtils.isNotBlank(timeInForce) && ApiTimeInForce.formValue(timeInForce.toUpperCase()) == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_TIF);
        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, PARAM_QUANTITY_NAME);
        }

        if (Strings.isNullOrEmpty(newClientOrderId)) {
            newClientOrderId = System.currentTimeMillis() + "" + Thread.currentThread().getId();
        }

        try {
            NewOrderResult result = orderService.newOrder(header, 0L, AccountTypeEnum.MARGIN, 0, exchangeId, symbol, newClientOrderId,
                    side, type, price.stripTrailingZeros().toPlainString(), quantity.stripTrailingZeros().toPlainString(),
                    ApiTimeInForce.formValue(timeInForce.toUpperCase()), methodVersion, orderSource);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case ORDER_REQUEST_SYMBOL_INVALID:
                    throw new OpenApiException(ApiErrorCode.BAD_SYMBOL);
                case ORDER_PRICE_TOO_HIGH:
                    throw new OpenApiException(ApiErrorCode.ORDER_PRICE_TOO_HIGH);
                case ORDER_PRICE_TOO_SMALL:
                    throw new OpenApiException(ApiErrorCode.ORDER_PRICE_TOO_SMALL);
                case ORDER_PRICE_PRECISION_TOO_LONG:
                    throw new OpenApiException(ApiErrorCode.ORDER_PRICE_PRECISION_TOO_LONG);
                case ORDER_QUANTITY_TOO_BIG:
                    throw new OpenApiException(ApiErrorCode.ORDER_QUANTITY_TOO_BIG);
                case ORDER_QUANTITY_TOO_SMALL:
                    throw new OpenApiException(ApiErrorCode.ORDER_QUANTITY_TOO_SMALL);
                case ORDER_QUANTITY_PRECISION_TOO_LONG:
                    throw new OpenApiException(ApiErrorCode.ORDER_QUANTITY_PRECISION_TOO_LONG);
                case ORDER_PRICE_WAVE_EXCEED:
                    throw new OpenApiException(ApiErrorCode.ORDER_PRICE_WAVE_EXCEED);
                case ORDER_AMOUNT_TOO_SMALL:
                    throw new OpenApiException(ApiErrorCode.ORDER_AMOUNT_TOO_SMALL);
                case ORDER_AMOUNT_PRECISION_TOO_LONG:
                    throw new OpenApiException(ApiErrorCode.ORDER_AMOUNT_PRECISION_TOO_LONG);
                case INSUFFICIENT_BALANCE:
                    throw new OpenApiException(ApiErrorCode.INSUFFICIENT_BALANCE);
                case DUPLICATED_ORDER:
                    throw new OpenApiException(ApiErrorCode.DUPLICATED_ORDER);
                case ORDER_FAILED:
                    log.warn("order Exception: orgId {}, userId:{}, domain {}, symbol:{}, cid:{}, side:{}, type:{}, price:{}, qty:{}, tif:{}",
                            header.getOrgId(), header.getUserId(), Strings.nullToEmpty(header.getDomain()), symbol, newClientOrderId, side, type, price, quantity, timeInForce);
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
                case SYMBOL_PROHIBIT_ORDER:
                    throw new OpenApiException(ApiErrorCode.SYMBOL_PROHIBIT_ORDER);
                case CREATE_ORDER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.CREATE_ORDER_TIMEOUT);
                case FEATURE_SUSPENDED:
                    throw new OpenApiException(ApiErrorCode.FEATURE_SUSPENDED);
                default:
                    log.error("create margin order:[orgId {}, userId:{}, domain {},  symbol:{}, cid:{}, side:{}, type:{}, price:{}, qty:{}, tif:{}] response error:{}",
                            header.getOrgId(), header.getUserId(), Strings.nullToEmpty(header.getDomain()), symbol, newClientOrderId, side, type, price, quantity, timeInForce,
                            errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.CREATE_ORDER_FAILED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(" margin order Exception: request:[orgId {}, userId:{}, domain {}, symbol:{}, cid:{}, side:{}, type:{}, price:{}, qty:{}, tif:{}]",
                    header.getOrgId(), header.getUserId(), Strings.nullToEmpty(header.getDomain()), symbol, newClientOrderId, side, type, price, quantity, timeInForce, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        } finally {
            MDC.remove(LogBizConstants.ORDER_ID);
        }

    }

    /**
     * 获取订单信息
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @GetMapping(value = "/order")
    public String queryOrder(Header header,
                             @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                             @RequestParam(name = "origClientOrderId", required = false, defaultValue = "0") String origClientOrderId,
                             @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                             @RequestParam(name = "timestamp", required = false) Long timestamp,
                             HttpServletRequest request) {
        try {
            QueryOrderResult result = orderService.getOrder(header, 0L, AccountTypeEnum.MARGIN, 0, origClientOrderId, orderId);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case ORDER_NOT_FOUND:
                    throw new OpenApiException(ApiErrorCode.NO_SUCH_ORDER);
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query margin order response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query margin order Exception: , orderId{}, clientOrderId:{}", orderId, origClientOrderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 撤单
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @DeleteMapping(value = "/order")
    public String cancelOrder(Header header,
                              @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                              @RequestParam(name = "clientOrderId", required = false, defaultValue = "0") String clientOrderId,
                              @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                              @RequestParam(name = "timestamp", required = false) Long timestamp,
                              @RequestParam(name = "methodVersion", required = false, defaultValue = "") String methodVersion,
                              HttpServletRequest request) {
        MDC.put(LogBizConstants.ORDER_ID, (orderId != null && orderId != 0) ? orderId.toString() : clientOrderId);
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        try {
            CancelOrderResult result = orderService.cancelOrder(header, 0L, AccountTypeEnum.MARGIN, 0, clientOrderId, orderId, methodVersion);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case ORDER_HAS_BEEN_FILLED:
                    // todo
                    throw new OpenApiException(ApiErrorCode.ORDER_HAS_FILLED);
                case ORDER_NOT_FOUND:
                    throw new OpenApiException(ApiErrorCode.NO_SUCH_ORDER);
                case CANCEL_ORDER_CANCELLED:
                    throw new OpenApiException(ApiErrorCode.ORDER_CANCELLED);
                case CANCEL_ORDER_FINISHED:
                    throw new OpenApiException(ApiErrorCode.ORDER_HAS_FILLED);
                case CANCEL_ORDER_REJECTED:
                    throw new OpenApiException(ApiErrorCode.ORDER_NOT_FOUND_ON_ORDER_BOOK);
                case CANCEL_ORDER_LOCKED:
                    throw new OpenApiException(ApiErrorCode.ORDER_LOCKED);
                case CANCEL_ORDER_UNSUPPORTED_ORDER_TYPE:
                    throw new OpenApiException(ApiErrorCode.UNSUPPORTED_ORDER_TYPE_UNSUPPORTED_CANCEL);
                case CANCEL_ORDER_ARCHIVED:
                    throw new OpenApiException(ApiErrorCode.ORDER_NOT_FOUND_ON_ORDER_BOOK);
                case CANCEL_ORDER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.CANCEL_ORDER_TIMEOUT);
                default:
                    log.warn("cancel margin order[{}] response error:{}",
                            header.getOrgId() + "-" + header.getUserId() + "-" + orderId + "-" + clientOrderId, errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.CANCEL_ORDER_FAILED);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            MDC.remove(LogBizConstants.ORDER_ID);
        }
    }

    /**
     * 获取当前委托单
     *
     * @param recvWindow
     * @param timestamp
     * @return
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = "/openOrders")
    public String queryOpenOrders(Header header,
                                  @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol,
                                  @RequestParam(name = "side", required = false, defaultValue = "") String side,
                                  @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                                  @RequestParam(name = "limit", required = false) Integer limit,
                                  @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                  @RequestParam(name = "timestamp", required = false) Long timestamp,
                                  HttpServletRequest request) {
        // 最大查询数量 500 默认500
        if (limit == null || limit <= 0) {
            limit = MAX_QUERY_ORDERS_LIMIT;
        }
        try {
            List<QueryOrderResult> list = orderService.queryOrders(header, OrderQueryType.CURRENT, 0L, AccountTypeEnum.MARGIN, 0,
                    symbol, side, orderId, 0L, 0L, 0L, limit);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query open orders response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query open orders Exception:  symbol{}, fromOrderId:{}", symbol, orderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取历史订单
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = "/historyOrders")
    public String queryHistoryOrders(Header header,
                                     @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol,
                                     @RequestParam(name = "side", required = false, defaultValue = "") String side,
                                     @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                                     @RequestParam(name = "startTime", required = false, defaultValue = "0") Long startTime,
                                     @RequestParam(name = "endTime", required = false, defaultValue = "0") Long endTime,
                                     @RequestParam(name = "limit", required = false, defaultValue = "500") Integer limit,
                                     @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                     @RequestParam(name = "timestamp", required = false) Long timestamp,
                                     HttpServletRequest request) {
        // 最大查询数量 500 默认500
        if (limit == null || limit <= 0) {
            limit = MAX_QUERY_ORDERS_LIMIT;
        }
        try {
            List<QueryOrderResult> list = orderService.queryOrders(header, OrderQueryType.HISTORY, 0L, AccountTypeEnum.MARGIN, 0,
                    symbol, side, orderId, 0L, startTime, endTime, limit);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query history margin orders response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query history margin orders Exception:  symbol{}, fromOrderId:{}", symbol, orderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取成交记录
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = "/myTrades")
    public String myTrades(Header header,
                           @RequestParam(name = "symbol", required = false, defaultValue = "") String symbolId,
                           @RequestParam(name = "fromId", required = false, defaultValue = "0") Long fromId,
                           @RequestParam(name = "toId", required = false, defaultValue = "0") Long toId,
                           @RequestParam(name = "startTime", required = false, defaultValue = "0") Long startTime,
                           @RequestParam(name = "endTime", required = false, defaultValue = "0") Long endTime,
                           @RequestParam(name = "limit", required = false, defaultValue = "500") Integer limit,
                           @RequestParam(name = "withIsNormal", required = false) String withIsNormal,
                           @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                           @RequestParam(name = "timestamp", required = false) Long timestamp,
                           HttpServletRequest request) {
        // 最大查询数量 500 默认500
        if (limit == null || limit > MAX_QUERY_TRADES_LIMIT) {
            limit = DEFAULT_QUERY_TRADES_LIMIT;
        }
        try {
            List<TradeResult> matchResultList = orderService.queryMatchInfo(header, 0L, AccountTypeEnum.MARGIN, 0,
                    symbolId, fromId, toId, startTime, endTime, limit, withIsNormal);

            return ResultUtils.toRestJSONString(matchResultList);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query margin trades[{}] response error:{}",
                            header.getOrgId() + "-" + header.getUserId() + "-" + AccountTypeEnum.MARGIN.name() + "-" + 0,
                            errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query margin trades Exception: startTime:{}, endTime:{}, fromId{}", startTime, endTime, fromId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取账户信息
     *
     * @return
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = "/account")
    public String accountInfo(Header header,
                              @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                              @RequestParam(name = "timestamp", required = false) Long timestamp,
                              HttpServletRequest request) {
        try {
            AccountResult result = accountService.getAccount(header, 0L, AccountTypeEnum.MARGIN, 0);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    log.error("request margin account[{}] info timeout", header.getOrgId() + "-" + header.getUserId() + "-" + AccountTypeEnum.MARGIN.name() + "-" + 0);
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                case GRPC_SERVER_SYSTEM_ERROR:
                    log.error("request margin  account[{}] info has system error", header.getOrgId() + "-" + header.getUserId() + "-" + AccountTypeEnum.MARGIN.name() + "-" + 0);
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
                default:
                    log.error("request margin account info response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("get margin account info Exception: ", e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 开通杠杆账户
     *
     * @param header
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = "/open")
    public String openMargin(Header header,
                             @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                             @RequestParam(name = "timestamp", required = false) Long timestamp,
                             HttpServletRequest request) {
        try {
            marginService.saveUserMarginContract(header, "margin");
            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("success", Boolean.TRUE);
            return JsonUtil.defaultGson().toJson(dataObj);
        } catch (Exception e) {
            log.error("open margin account  Exception: ", e);
            throw new OpenApiException(ApiErrorCode.OPEN_MARGIN_ACCOUNT_ERROR);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = "/safety")
    public String getMarginSafety(Header header,
                                  @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                  @RequestParam(name = "timestamp", required = false) Long timestamp,
                                  HttpServletRequest request) {
        try {
            GetMarginSafetyResponse resp = marginService.getMarginSafety(header);
            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("safety", resp.getMarginSafety().getSafety());
            return JsonUtil.defaultGson().toJson(dataObj);
        } catch (Exception e) {
            log.error("get margin safety  Exception: ", e);
            throw new OpenApiException(ApiErrorCode.OPEN_MARGIN_ACCOUNT_ERROR);
        }

    }

    /**
     * 查询Margin Token
     *
     * @param header
     * @return
     */
    @RequestMapping(value = "/token")
    public String getMarginTokens(Header header) {
        List<MarginTokenResult> results = marginService.getMarginToken(header);
        return JsonUtil.defaultGson().toJson(results);
    }

    /**
     * 查询风控配置
     *
     * @param header
     * @return
     */
    @RequestMapping(value = "/riskConfig")
    public String getMarginRisk(Header header) {
        MarginRiskResult result = marginService.getMarginRisk(header);
        return JsonUtil.defaultGson().toJson(result);
    }

    /**
     * 账户可借查询
     *
     * @param header
     * @param tokenId
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 10)
    @RequestMapping(value = "/loanable")
    public String getMarginLoanable(Header header,
                                    @RequestParam(name = "tokenId", required = true) String tokenId,
                                    @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                    @RequestParam(name = "timestamp", required = false) Long timestamp,
                                    HttpServletRequest request) {
        try {
            GetLoanableResponse resp = marginService.getLoanable(header, tokenId);
            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("tokenId", resp.getTokenId());
            dataObj.addProperty("loanable", resp.getLoanable());
            return JsonUtil.defaultGson().toJson(dataObj);
        } catch (Exception e) {
            log.error("get margin loanable  Exception: ", e);
            throw new OpenApiException(ApiErrorCode.GET_LOANABLE_ERROR);
        }
    }

    /**
     * 账户已借查询
     *
     * @param header
     * @param tokenId
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = "/loanPosition")
    public String getMarginLoanPosition(Header header,
                                        @RequestParam(name = "tokenId", required = false, defaultValue = "") String tokenId,
                                        @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                        @RequestParam(name = "timestamp", required = false) Long timestamp,
                                        HttpServletRequest request) {
        try {
            List<MarginLoanPositionResult> resp = marginService.getLoanPosition(header, tokenId);
            return JsonUtil.defaultGson().toJson(resp);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case MARGIN_TOKEN_NOT_BORROW:
                    throw new OpenApiException(ApiErrorCode.MARGIN_TOKEN_NOT_BORROW);
                default:
                    log.error("request query loan position response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.GET_LOAN_POSITION_ERROR);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(" query loan position Exception:  orgId{}, tokenId{}", header.getOrgId(), tokenId);
            throw new OpenApiException(ApiErrorCode.GET_LOAN_POSITION_ERROR);
        }
    }

    /**
     * 账户可出金数量查询
     *
     * @param header
     * @param tokenId
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = AccountType.COMMON)
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 10)
    @RequestMapping(value = "/availWithdraw")
    public String getMarginAvailWithdraw(Header header,
                                         @RequestParam(name = "tokenId", required = true) String tokenId,
                                         @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                         @RequestParam(name = "timestamp", required = false) Long timestamp,
                                         HttpServletRequest request) {
        try {
            GetAvailWithdrawAmountResponse resp = marginService.getAvailWithdrawAmount(header, tokenId);
            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("tokenId", tokenId);
            dataObj.addProperty("availWithdrawAmount", resp.getAvailWithdrawAmount());
            return JsonUtil.defaultGson().toJson(dataObj);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case MARGIN_TOKEN_NOT_BORROW:
                    throw new OpenApiException(ApiErrorCode.MARGIN_TOKEN_NOT_WITHDRAW);
                default:
                    log.error("request get avail withdraw response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.GET_AVAIL_WITHDRAW_ERROR);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(" get avail withdraw  Exception:  orgId{}, tokenId{}", header.getOrgId(), tokenId);
            throw new OpenApiException(ApiErrorCode.GET_AVAIL_WITHDRAW_ERROR);
        }
    }

    /**
     * 账户内转账（账户出入金）
     *
     * @param header
     * @param fromAccountType
     * @param toAccountType
     * @param tokenId
     * @param amount
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = AccountType.COMMON)
    @LimitAuth(limitTypes = RateLimitType.REQUEST_WEIGHT, weight = 10)
    @RequestMapping(value = "/transfer")
    public String marginTransfer(Header header,
                                 @RequestParam(name = "fromAccountType", required = true) Integer fromAccountType,
                                 @RequestParam(name = "toAccountType", required = true) Integer toAccountType,
                                 @RequestParam(name = "tokenId", required = true) String tokenId,
                                 @RequestParam(name = "amount", required = true) String amount,
                                 @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                 @RequestParam(name = "timestamp", required = false) Long timestamp,
                                 HttpServletRequest request) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        try {
            if (fromAccountType.equals(toAccountType)) {
                JsonObject dataObj = new JsonObject();
                dataObj.addProperty("success", Boolean.FALSE);
                return JsonUtil.defaultGson().toJson(dataObj);
            }
            if ((fromAccountType == AccountType.MAIN.value() && toAccountType == AccountType.MARGIN.value()) //入金
                    || (fromAccountType == AccountType.MARGIN.value() && toAccountType == AccountType.MAIN.value())) { //出金
                accountService.subAccountTransfer(header, fromAccountType, 0, toAccountType, 0, tokenId, amount);
                JsonObject dataObj = new JsonObject();
                dataObj.addProperty("success", Boolean.TRUE);
                return JsonUtil.defaultGson().toJson(dataObj);
            } else {
                log.warn("margin transfer account type error fromAccountType:{} toAccountType:{}", fromAccountType, toAccountType);
                throw new OpenApiException(ApiErrorCode.BAD_REQUEST);
            }
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case ACCOUNT_NOT_EXIST:
                    throw new OpenApiException(ApiErrorCode.ACCOUNT_NOT_EXIST);
                case MARGIN_WITHDRAW_FAILED:
                    throw new OpenApiException(ApiErrorCode.MARGIN_WITHDRAW_ERROR);
                case MARGIN_AVAIL_WITHDRAW_NOT_ENOUGH_FAILED:
                    throw new OpenApiException(ApiErrorCode.MARGIN_AVAIL_WITHDRAW_NOT_ENOUGH_FAILED);
                case MARGIN_TOKEN_NOT_BORROW:
                    throw new OpenApiException(ApiErrorCode.MARGIN_TOKEN_NOT_WITHDRAW);
                case INSUFFICIENT_BALANCE:
                    throw new OpenApiException(ApiErrorCode.INSUFFICIENT_BALANCE);
                default:
                    log.error("request margin transfer response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.MARGIN_WITHDRAW_ERROR);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(" margin transfer Exception:  orgId:{}, userId:{}", header.getOrgId(), header.getUserId());
            throw new OpenApiException(ApiErrorCode.MARGIN_WITHDRAW_ERROR);
        }
    }

    /**
     * 借币
     *
     * @param header
     * @param clientOrderId
     * @param tokenId
     * @param loanAmount
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(checkRecvWindow = true, requiredAccountTypes = AccountType.COMMON)
    @LimitAuth(limitTypes = RateLimitType.REQUEST_WEIGHT, weight = 5)
    @PostMapping(value = "/loan")
    public String loan(Header header,
                       @RequestParam(name = "clientOrderId", required = false, defaultValue = "") String clientOrderId,
                       @RequestParam(name = "tokenId", required = true) String tokenId,
                       @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_AMOUNT)
                       @RequestParam(name = "loanAmount", required = true) String loanAmount,
                       @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                       @RequestParam(name = "timestamp", required = false) Long timestamp,
                       HttpServletRequest request) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        if (Strings.isNullOrEmpty(clientOrderId)) {
            clientOrderId = System.currentTimeMillis() + "" + Thread.currentThread().getId();
        }
        try {
            LoanResponse response = marginService.loan(header, clientOrderId, loanAmount, tokenId);
            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("loanOrderId", response.getLoanOrderId());
            dataObj.addProperty("clientOrderId", clientOrderId);
            return JsonUtil.defaultGson().toJson(dataObj);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case MARGIN_TOKEN_NOT_BORROW:
                    throw new OpenApiException(ApiErrorCode.MARGIN_TOKEN_NOT_BORROW);
                case MARGIN_LOAN_AMOUNT_TOO_BIG_OR_SMALL:
                    throw new OpenApiException(ApiErrorCode.MARGIN_LOAN_AMOUNT_TOO_BIG_OR_SMALL);
                case MARGIN_LOAN_AMOUNT_PRECISION_TOO_LONG:
                    throw new OpenApiException(ApiErrorCode.MARGIN_LOAN_AMOUNT_PRECISION_TOO_LONG);
                default:
                    log.error("request loan response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.LOAN_ERROR);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("loan Exception: orgId{}, tokenId{}", header.getOrgId(), tokenId);
            throw new OpenApiException(ApiErrorCode.LOAN_ERROR);
        }
    }

    /**
     * 还币
     *
     * @param header
     * @param clientOrderId
     * @param loanOrderId
     * @param repayType
     * @param repayAmount
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = AccountType.COMMON)
    @LimitAuth(limitTypes = RateLimitType.REQUEST_WEIGHT, weight = 5)
    @PostMapping(value = "/repay")
    public String repay(Header header,
                        @RequestParam(name = "clientOrderId", required = false, defaultValue = "") String clientOrderId,
                        @RequestParam(name = "loanOrderId", required = true) Long loanOrderId,
                        @RequestParam(name = "repayType", required = false, defaultValue = "2") Integer repayType,
                        @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_AMOUNT)
                        @RequestParam(name = "repayAmount", required = false, defaultValue = "0") String repayAmount,
                        @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                        @RequestParam(name = "timestamp", required = false) Long timestamp,
                        HttpServletRequest request) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        if (Strings.isNullOrEmpty(clientOrderId)) {
            clientOrderId = System.currentTimeMillis() + "" + Thread.currentThread().getId();
        }
        if (repayType != MarginRepayTypeEnum.ALL_REPAY_VALUE && new BigDecimal(repayAmount).compareTo(BigDecimal.ZERO) <= 0) {
            throw new OpenApiException(ApiErrorCode.BAD_REQUEST);
        }
        try {
            RepayByLoanIdResponse resp = marginService.repay(header, clientOrderId, loanOrderId, repayType, repayAmount);
            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("repayOrderId", resp.getRepayOrderId());
            dataObj.addProperty("clientOrderId", clientOrderId);
            return JsonUtil.defaultGson().toJson(dataObj);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case MARGIN_LOAN_ORDER_NOT_EXIST:
                    throw new OpenApiException(ApiErrorCode.MARGIN_LOAN_ORDER_NOT_EXIST);
                case MARGIN_LOAN_REPAY_AMOUNT_IS_SMALL:
                    throw new OpenApiException(ApiErrorCode.MARGIN_LOAN_REPAY_AMOUNT_IS_SMALL);
                default:
                    log.error("request repay response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.REPAY_ERROR);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("repay Exception: orgId{}, loanOrderId{}", header.getOrgId(), loanOrderId);
            throw new OpenApiException(ApiErrorCode.REPAY_ERROR);
        }

    }

    /**
     * 保证金查询 （包含可用保证金&占用保证金）
     *
     * @param header
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = AccountType.COMMON)
    @LimitAuth(limitTypes = RateLimitType.REQUEST_WEIGHT, weight = 5)
    @RequestMapping(value = "/allPosition")
    public String getAllPosition(Header header,
                                 @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                 @RequestParam(name = "timestamp", required = false) Long timestamp,
                                 HttpServletRequest request) {
        try {
            MarginAllPositionResult result = marginService.getAllPosition(header);
            return JsonUtil.defaultGson().toJson(result);
        } catch (Exception e) {
            log.error("getAllPosition Exception: orgId{}", header.getOrgId());
            throw new OpenApiException(ApiErrorCode.GET_MARGIN_ALL_POSITION_ERROR);
        }
    }

    /**
     * 查询借币记录列表
     *
     * @param header
     * @param tokenId
     * @param status
     * @param fromLoanId
     * @param endLoanId
     * @param limit
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = AccountType.COMMON)
    @LimitAuth(limitTypes = RateLimitType.REQUEST_WEIGHT, weight = 5)
    @RequestMapping(value = "/loanOrders")
    public String queryMarginLoanOrders(Header header,
                                        @RequestParam(name = "tokenId", required = false, defaultValue = "") String tokenId,
                                        @RequestParam(name = "status", required = false, defaultValue = "0") Integer status,
                                        @RequestParam(name = "fromLoanId", required = false, defaultValue = "0") Long fromLoanId,
                                        @RequestParam(name = "endLoanId", required = false, defaultValue = "0") Long endLoanId,
                                        @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit,
                                        @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                        @RequestParam(name = "timestamp", required = false) Long timestamp,
                                        HttpServletRequest request) {
        try {
            List<MarginLoanOrderResult> resp = marginService.queryLoanOrders(header, tokenId, status, fromLoanId, endLoanId, limit);
            return JsonUtil.defaultGson().toJson(resp);
        } catch (Exception e) {
            log.error("queryMarginLoanOrders Exception: orgId{}", header.getOrgId());
            throw new OpenApiException(ApiErrorCode.GET_LOAN_ORDER_ERROR);
        }
    }

    /**
     * 获取借币记录
     *
     * @param header
     * @param loanOrderId
     * @param recvWindow
     * @param timestamp
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = AccountType.COMMON)
    @LimitAuth(limitTypes = RateLimitType.REQUEST_WEIGHT, weight = 5)
    @RequestMapping(value = "/getLoanOrder")
    public String getLoanOrderById(Header header,
                                   @RequestParam(name = "loanOrderId", required = true) Long loanOrderId,
                                   @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                   @RequestParam(name = "timestamp", required = false) Long timestamp,
                                   HttpServletRequest request) {
        try {
            MarginLoanOrderResult result = marginService.getLoanOrder(header, loanOrderId);
            if (result == null) {
                throw new OpenApiException(ApiErrorCode.LOAN_ORDER_NOT_EXIT);
            }
            return JsonUtil.defaultGson().toJson(result);
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("queryMarginLoanOrders Exception: orgId{}", header.getOrgId());
            throw new OpenApiException(ApiErrorCode.GET_LOAN_ORDER_ERROR);
        }
    }

    @SignAuth(requiredAccountTypes = AccountType.COMMON)
    @LimitAuth(limitTypes = RateLimitType.REQUEST_WEIGHT, weight = 5)
    @RequestMapping(value = "/repayOrders")
    public String queryRepayOrders(Header header,
                                   @RequestParam(name = "tokenId", required = false, defaultValue = "") String tokenId,
                                   @RequestParam(name = "fromRepayId", required = false, defaultValue = "0") Long fromRepayId,
                                   @RequestParam(name = "endRepayId", required = false, defaultValue = "0") Long endRepayId,
                                   @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit,
                                   @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                   @RequestParam(name = "timestamp", required = false) Long timestamp,
                                   HttpServletRequest request) {
        try {
            List<MarginRepayOrderResult> results = marginService.queryRepayOrders(header, tokenId, fromRepayId, endRepayId, limit);
            return JsonUtil.defaultGson().toJson(results);
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("queryRepayOrders Exception: orgId{}", header.getOrgId());
            throw new OpenApiException(ApiErrorCode.GET_REPAY_ORDER_ERROR);
        }
    }

    @SignAuth(requiredAccountTypes = AccountType.COMMON)
    @LimitAuth(limitTypes = RateLimitType.REQUEST_WEIGHT, weight = 5)
    @RequestMapping(value = "/getRepayOrder")
    public String getRepayOrderByLoanId(Header header,
                                        @RequestParam(name = "loanOrderId", required = true) Long loanOrderId,
                                        @RequestParam(name = "fromRepayId", required = false, defaultValue = "0") Long fromRepayId,
                                        @RequestParam(name = "endRepayId", required = false, defaultValue = "0") Long endRepayId,
                                        @RequestParam(name = "limit", required = false, defaultValue = "50") Integer limit,
                                        @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                        @RequestParam(name = "timestamp", required = false) Long timestamp,
                                        HttpServletRequest request) {
        try {
            List<MarginRepayOrderResult> results = marginService.getRepayOrder(header, loanOrderId, fromRepayId, endRepayId, limit);
            return JsonUtil.defaultGson().toJson(results);
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("getRepayOrderByLoanId Exception: orgId{}", header.getOrgId());
            throw new OpenApiException(ApiErrorCode.GET_REPAY_ORDER_ERROR);
        }
    }

    /**
     * 查询杠杆balance flow
     *
     * @param header
     * @param tokenId
     * @param fromFlowId
     * @param endFlowId
     * @param startTime
     * @param endTime
     * @param limit
     * @param request
     * @return
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = "/balanceFlow")
    public String queryMarginBalanceFlow(Header header,
                                         @RequestParam(required = false, defaultValue = "") String tokenId,
                                         @RequestParam(required = false, defaultValue = "") Integer flowType,
                                         @RequestParam(required = false, defaultValue = "0") Long fromFlowId,
                                         @RequestParam(required = false, defaultValue = "0") Long endFlowId,
                                         @RequestParam(required = false, defaultValue = "0") Long startTime,
                                         @RequestParam(required = false, defaultValue = "0") Long endTime,
                                         @RequestParam(required = false, defaultValue = "50") Integer limit,
                                         HttpServletRequest request) {
        List<BalanceFlowResult> flowList = accountService.queryBalanceFlow(header, tokenId, flowType, fromFlowId, endFlowId, startTime, endTime, limit,
                AccountTypeEnum.MARGIN, 0);
        return JsonUtil.defaultGson().toJson(flowList);
    }
}
