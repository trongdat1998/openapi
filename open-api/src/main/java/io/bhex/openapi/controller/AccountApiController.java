package io.bhex.openapi.controller;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.bhex.base.log.LogBizConstants;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.broker.core.validate.HbtcParamType;
import io.bhex.broker.core.validate.ValidHbtcParam;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.common.OpenApiSpecialPermission;
import io.bhex.broker.grpc.order.OrderQueryType;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.constant.TransferConstant;
import io.bhex.openapi.domain.AccountType;
import io.bhex.openapi.domain.BalanceFlowResult;
import io.bhex.openapi.domain.BestOrderResult;
import io.bhex.openapi.domain.DepositAddressResult;
import io.bhex.openapi.domain.DepositOrderResult;
import io.bhex.openapi.domain.EtfPriceResult;
import io.bhex.openapi.domain.SymbolResult;
import io.bhex.openapi.domain.TransferResult;
import io.bhex.openapi.domain.WithdrawDetailResult;
import io.bhex.openapi.domain.WithdrawResult;
import io.bhex.openapi.domain.api.enums.ApiAssetType;
import io.bhex.openapi.domain.api.enums.ApiOrderSide;
import io.bhex.openapi.domain.api.enums.ApiOrderType;
import io.bhex.openapi.domain.api.enums.ApiTimeInForce;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.domain.api.result.AccountResult;
import io.bhex.openapi.domain.api.result.CancelOrderResult;
import io.bhex.openapi.domain.api.result.FastCancelOrderResult;
import io.bhex.openapi.domain.api.result.NewOrderResult;
import io.bhex.openapi.domain.api.result.QueryOrderResult;
import io.bhex.openapi.domain.api.result.TradeResult;
import io.bhex.openapi.interceptor.OpenApiInterceptor;
import io.bhex.openapi.interceptor.annotation.LimitAuth;
import io.bhex.openapi.interceptor.annotation.SignAuth;
import io.bhex.openapi.service.AccountService;
import io.bhex.openapi.service.BalanceService;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.service.OrderService;
import io.bhex.openapi.util.ReadOnlyApiKeyCheckUtil;
import io.bhex.openapi.util.ResultUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping(value = {"/openapi", "/openapi/account"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AccountApiController {

    private static final String BEST_ORDER_WHITE_LIST_KEY = "_bestOrderWhiteAccount";

    private static final Integer MAX_QUERY_ORDERS_LIMIT = 500;
    private static final Integer DEFAULT_QUERY_TRADES_LIMIT = 500;
    private static final Integer MAX_QUERY_TRADES_LIMIT = 1000;

    private static final String PARAM_SYMBOL_NAME = "symbol";
    private static final String PARAM_QUANTITY_NAME = "quantity";
    private static final String PARAM_ASSET_TYPE_NAME = "assetType";
    private static final String PARAM_ICEBERG_QTY_NAME = "icebergQty";
    private static final String PARAM_STOP_PRICE_NAME = "stopPrice";

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private BasicService basicService;

    @Resource
    private BalanceService balanceService;

    /**
     * test order
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/checkApiKey", "/v1/checkApiKey"})
    public String checkApiKey(Header header,
                              @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                              @RequestParam(name = "timestamp", required = false) Long timestamp,
                              HttpServletRequest request) {
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        JsonObject dataObj = new JsonObject();
        if (accountType == AccountTypeEnum.COIN && accountIndex == 0) {
            dataObj.addProperty("accountType", "master");
        } else {
            if (accountType == AccountTypeEnum.COIN) {
                dataObj.addProperty("accountType", "spot");
            } else if (accountType == AccountTypeEnum.FUTURE) {
                dataObj.addProperty("accountType", "contract");
            }
        }
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    /**
     * 创建订单
     *
     * @param symbol
     * @param assetType
     * @param side
     * @param type
     * @param timeInForce
     * @param quantity
     * @param price
     * @param newClientOrderId
     * @param stopPrice
     * @param icebergQty
     * @param recvWindow
     * @param timestamp
     * @return
     */
    @SignAuth(checkRecvWindow = true, requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT, RateLimitType.ORDERS}, weight = 1)
    @PostMapping(value = {"/order", "/v1/order"})
    public String newOrder(Header header,
                           @RequestParam(name = "symbol", required = true) String symbol,
                           @RequestParam(name = "side", required = true) String side,
                           @RequestParam(name = "type", required = true) String type,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_QUANTITY)
                           @RequestParam(name = "quantity", required = true) BigDecimal quantity,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_PRICE)
                           @RequestParam(name = "price", required = false, defaultValue = "0") BigDecimal price,
                           @RequestParam(name = "assetType", required = false) String assetType,
                           @RequestParam(name = "timeInForce", required = false, defaultValue = "GTC") String timeInForce,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.CLIENT_ORDER_ID)
                           @RequestParam(name = "newClientOrderId", required = false, defaultValue = "") String newClientOrderId,
                           @RequestParam(name = "stopPrice", required = false) BigDecimal stopPrice,
                           @RequestParam(name = "icebergQty", required = false) BigDecimal icebergQty,
                           @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                           @RequestParam(name = "timestamp", required = true) Long timestamp,
                           @RequestParam(name = "isTest", required = false, defaultValue = "false") Boolean isTest,
                           @RequestParam(name = "methodVersion", required = false, defaultValue = "") String methodVersion,
                           @RequestParam(name = "orderSource", required = false, defaultValue = "") String orderSource,
                           HttpServletRequest request) {
        MDC.put(LogBizConstants.ORDER_ID, newClientOrderId);

        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);

        SymbolResult symbolResult = basicService.querySymbol(header, symbol);
        if (symbolResult == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, PARAM_SYMBOL_NAME);
        }
//        if (symbolResult.getForbidOpenapiTrade()) {
//            throw new OpenApiException(ApiErrorCode.SYMBOL_PROHIBIT_ORDER);
//        }
        Long exchangeId = symbolResult.getExchangeId();

        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);

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

        // assetType 只支持CASH
        if (StringUtils.isNotBlank(assetType) && ApiAssetType.fromValue(assetType.toLowerCase()) == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, PARAM_ASSET_TYPE_NAME);
        }

//        // icebergQty 必须不存在或为0
//        if (icebergQty != null && icebergQty != 0) {
//            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, PARAM_ICEBERG_QTY_NAME);
//        }
//
//        // stopPrice 必须不存在或为0
//        if (stopPrice != null && stopPrice != 0) {
//            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, PARAM_STOP_PRICE_NAME);
//        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, PARAM_QUANTITY_NAME);
        }

        if (Strings.isNullOrEmpty(newClientOrderId)) {
            newClientOrderId = System.currentTimeMillis() + "" + Thread.currentThread().getId();
        }

        if (isTest) {
            return "{}";
        }

        try {
            NewOrderResult result = orderService.newOrder(header, accountId, accountType, accountIndex, exchangeId, symbol, newClientOrderId,
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
                    log.warn("order Exception: orgId {}, userId:{}, domain {}, accountId:{}, symbol:{}, cid:{}, side:{}, type:{}, price:{}, qty:{}, tif:{}",
                            header.getOrgId(), header.getUserId(), Strings.nullToEmpty(header.getDomain()), accountId, symbol, newClientOrderId, side, type, price, quantity, timeInForce);
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
                case SYMBOL_PROHIBIT_ORDER:
                    throw new OpenApiException(ApiErrorCode.SYMBOL_PROHIBIT_ORDER);
                case CREATE_ORDER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.CREATE_ORDER_TIMEOUT);
                case FEATURE_SUSPENDED:
                    throw new OpenApiException(ApiErrorCode.FEATURE_SUSPENDED);
                case ORDER_LIMIT_MAKER_FAILED:
                    throw new OpenApiException(ApiErrorCode.CREATE_LIMIT_MAKER_ORDER_FAILED);
                case SYMBOL_OPENAPI_TRADE_FORBIDDEN:
                    throw new OpenApiException(ApiErrorCode.SYMBOL_API_TRADING_NOT_AVAILABLE);
                default:
                    log.warn("create order:[orgId {}, userId:{}, domain {}, accountId:{}, symbol:{}, cid:{}, side:{}, type:{}, price:{}, qty:{}, tif:{}] response error:{}",
                            header.getOrgId(), header.getUserId(), Strings.nullToEmpty(header.getDomain()), accountId, symbol, newClientOrderId, side, type, price, quantity, timeInForce,
                            errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.CREATE_ORDER_FAILED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error(" order Exception: request:[orgId {}, userId:{}, domain {}, accountId:{}, symbol:{}, cid:{}, side:{}, type:{}, price:{}, qty:{}, tif:{}]",
                    header.getOrgId(), header.getUserId(), Strings.nullToEmpty(header.getDomain()), accountId, symbol, newClientOrderId, side, type, price, quantity, timeInForce, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        } finally {
            MDC.remove(LogBizConstants.ORDER_ID);
        }
    }

    /**
     * test order
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @PostMapping(value = {"/order/test", "/v1/order/test"})
    public String testNewOrder(Header header,
                               @RequestParam(name = "symbol", required = true) String symbol,
                               @RequestParam(name = "side", required = true) String side,
                               @RequestParam(name = "type", required = true) String type,
                               @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_QUANTITY)
                               @RequestParam(name = "quantity", required = true) BigDecimal quantity,
                               @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_PRICE)
                               @RequestParam(name = "price", required = false, defaultValue = "0") BigDecimal price,
                               @RequestParam(name = "assetType", required = false) String assetType,
                               @RequestParam(name = "timeInForce", required = false, defaultValue = "GTC") String timeInForce,
                               @RequestParam(name = "newClientOrderId", required = false, defaultValue = "") String newClientOrderId,
                               @RequestParam(name = "stopPrice", required = false) BigDecimal stopPrice,
                               @RequestParam(name = "icebergQty", required = false) BigDecimal icebergQty,
                               @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                               @RequestParam(name = "timestamp", required = false) Long timestamp,
                               HttpServletRequest request) {
        return newOrder(header, symbol, side, type, quantity, price, assetType, timeInForce, newClientOrderId, stopPrice,
                icebergQty, recvWindow, timestamp, true, "", "", request);
    }

    /**
     * 获取订单信息
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @GetMapping(value = {"/order", "/v1/order"})
    public String queryOrder(Header header,
                             @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                             @RequestParam(name = "origClientOrderId", required = false, defaultValue = "0") String origClientOrderId,
                             @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                             @RequestParam(name = "timestamp", required = false) Long timestamp,
                             HttpServletRequest request) {
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            QueryOrderResult result = orderService.getOrder(header, accountId, accountType, accountIndex, origClientOrderId, orderId);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case ORDER_NOT_FOUND:
                    throw new OpenApiException(ApiErrorCode.NO_SUCH_ORDER);
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query order response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query order Exception: accountId:{}, orderId{}, clientOrderId:{}", accountId, orderId, origClientOrderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 撤单
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @DeleteMapping(value = {"/order", "/v1/order"})
    public String cancelOrder(Header header,
                              @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                              @RequestParam(name = "clientOrderId", required = false, defaultValue = "0") String clientOrderId,
                              @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                              @RequestParam(name = "timestamp", required = false) Long timestamp,
                              @RequestParam(name = "methodVersion", required = false, defaultValue = "") String methodVersion,
                              @RequestParam(name = "fastCancel", required = false, defaultValue = "0") Integer fastCancel,
                              @RequestParam(name = "symbolId", required = false, defaultValue = "") String symbolId,
                              @RequestParam(name = "securityType", required = false, defaultValue = "1") Integer securityType,
                              HttpServletRequest request) {
        MDC.put(LogBizConstants.ORDER_ID, (orderId != null && orderId != 0) ? orderId.toString() : clientOrderId);
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            if (fastCancel >= 1 && StringUtils.isNotEmpty(symbolId)) {
                FastCancelOrderResult result = orderService.fastCancelOrder(header, accountId, clientOrderId, orderId, symbolId, securityType, accountType, accountIndex);
                return ResultUtils.toRestJSONString(result);
            }
            CancelOrderResult result = orderService.cancelOrder(header, accountId, accountType, accountIndex, clientOrderId, orderId, methodVersion);
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
                    log.warn("cancel order[{}] response error:{}",
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
     * 批量撤单
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/order/batchCancel", "/v1/order/batchCancel"}, method = {RequestMethod.GET, RequestMethod.DELETE})
    public String batchCancelOrder(Header header,
                                   @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol,
                                   @RequestParam(name = "side", required = false, defaultValue = "") String side,
                                   @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                   @RequestParam(name = "timestamp", required = false) Long timestamp,
                                   HttpServletRequest request) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            orderService.batchCancelOrder(header, accountId, accountType, accountIndex, symbol, side);
            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("success", Boolean.TRUE);
            return JsonUtil.defaultGson().toJson(dataObj);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 获取当前委托单
     *
     * @param recvWindow
     * @param timestamp
     * @return
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/openOrders", "/v1/openOrders"})
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
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            List<QueryOrderResult> list = orderService.queryOrders(header, OrderQueryType.CURRENT, accountId, accountType, accountIndex,
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
            log.error("query open orders Exception: accountId:{}, symbol{}, fromOrderId:{}", accountId, symbol, orderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @RequestMapping(value = {"/anyOpenOrders", "/v1/anyOpenOrders"})
    public String queryAnyOpenOrders(Header header,
                                     @RequestParam(name = "accountId") Long accountId,
                                     @RequestParam(name = "symbol") String symbol,
                                     @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                                     @RequestParam(name = "limit", required = false, defaultValue = "100") Integer limit,
                                     @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                     @RequestParam(name = "timestamp", required = false) Long timestamp,
                                     HttpServletRequest request, HttpServletResponse response) {
        Integer specialPermission = (Integer) request.getAttribute(OpenApiInterceptor.API_SPECIAL_PERMISSION_ATTR);
        if (!((specialPermission & OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE) == OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "wrong request";
        }
        // 最大查询数量 500 默认500
        if (limit == null || limit <= 0) {
            limit = MAX_QUERY_ORDERS_LIMIT;
        }
        try {
            List<QueryOrderResult> list = orderService.queryAnyOrders(header, OrderQueryType.CURRENT, accountId, symbol, orderId, 0L, 0L, 0L, limit);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query any open orders response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query any open orders Exception: accountId:{}, symbol{}, fromOrderId:{}", accountId, symbol, orderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取历史订单
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/historyOrders", "/v1/historyOrders"})
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
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            List<QueryOrderResult> list = orderService.queryOrders(header, OrderQueryType.HISTORY, accountId, accountType, accountIndex,
                    symbol, side, orderId, 0L, startTime, endTime, limit);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query history orders response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query history orders Exception: accountId:{}, symbol{}, fromOrderId:{}", accountId, symbol, orderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取历史订单
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @RequestMapping(value = {"/anyHistoryOrders", "/v1/anyHistoryOrders"})
    public String queryAnyHistoryOrders(Header header,
                                        @RequestParam(name = "accountId") Long accountId,
                                        @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol,
                                        @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                                        @RequestParam(name = "startTime", required = false, defaultValue = "0") Long startTime,
                                        @RequestParam(name = "endTime", required = false, defaultValue = "0") Long endTime,
                                        @RequestParam(name = "limit", required = false, defaultValue = "500") Integer limit,
                                        @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                        @RequestParam(name = "timestamp", required = false) Long timestamp,
                                        HttpServletRequest request, HttpServletResponse response) {
        Integer specialPermission = (Integer) request.getAttribute(OpenApiInterceptor.API_SPECIAL_PERMISSION_ATTR);
        if (!((specialPermission & OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE) == OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "wrong request";
        }
        // 最大查询数量 500 默认500
        if (limit == null || limit <= 0) {
            limit = MAX_QUERY_ORDERS_LIMIT;
        }
        try {
            List<QueryOrderResult> list = orderService.queryAnyOrders(header, OrderQueryType.HISTORY, accountId,
                    symbol, orderId, 0L, startTime, endTime, limit);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query history orders response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query history orders Exception: accountId:{}, symbol{}, fromOrderId:{}", accountId, symbol, orderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取账户信息
     *
     * @return
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/account", "/v1/account"})
    public String accountInfo(Header header,
                              @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                              @RequestParam(name = "timestamp", required = false) Long timestamp,
                              HttpServletRequest request) {
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            AccountResult result = accountService.getAccount(header, accountId, accountType, accountIndex);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    log.error("request account[{}] info timeout", header.getOrgId() + "-" + header.getUserId() + "-" + accountType.name() + "-" + accountIndex);
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                case GRPC_SERVER_SYSTEM_ERROR:
                    log.error("request account[{}] info has system error", header.getOrgId() + "-" + header.getUserId() + "-" + accountType.name() + "-" + accountIndex);
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
                default:
                    log.error("request account info response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("get account info Exception: accountId:{}", accountId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取成交记录
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/myTrades", "/v1/myTrades"})
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
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            List<TradeResult> matchResultList = orderService.queryMatchInfo(header, accountId, accountType, accountIndex,
                    symbolId, fromId, toId, startTime, endTime, limit, withIsNormal);

            return ResultUtils.toRestJSONString(matchResultList);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query trades[{}] response error:{}",
                            header.getOrgId() + "-" + header.getUserId() + "-" + accountType.name() + "-" + accountIndex,
                            errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query trades Exception: accountId:{}, startTime:{}, endTime:{}, fromId{}", accountId, startTime, endTime, fromId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取充币记录
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/depositOrders", "/v1/depositOrders"})
    public String depositOrders(Header header,
                                @RequestParam(required = false, defaultValue = "") String token,
                                @RequestParam(name = "startTime", required = false, defaultValue = "0") Long startTime,
                                @RequestParam(name = "endTime", required = false, defaultValue = "0") Long endTime,
                                @RequestParam(name = "fromId", required = false, defaultValue = "0") Long fromId,
                                @RequestParam(name = "limit", required = false, defaultValue = "500") Integer limit,
                                @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                @RequestParam(name = "timestamp", required = false) Long timestamp,
                                HttpServletRequest request) {
        // 最大查询数量 500 默认500
        if (limit == null || limit > MAX_QUERY_TRADES_LIMIT) {
            limit = DEFAULT_QUERY_TRADES_LIMIT;
        }
        try {
            List<DepositOrderResult> depositOrderResults = balanceService.queryDepositOrder(header, token,
                    fromId, startTime, endTime, limit);
            return ResultUtils.toRestJSONString(depositOrderResults);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query deposit orders response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query deposit orders Exception: userId:{}, startTime:{}, endTime:{}, fromId{}", header.getUserId(), startTime, endTime, fromId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取充币记录
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/withdrawalOrders", "/v1/withdrawalOrders"})
    public String withdrawalOrders(Header header,
                                   @RequestParam(required = false, defaultValue = "") String token,
                                   @RequestParam(name = "startTime", required = false, defaultValue = "0") Long startTime,
                                   @RequestParam(name = "endTime", required = false, defaultValue = "0") Long endTime,
                                   @RequestParam(name = "fromId", required = false, defaultValue = "0") Long fromId,
                                   @RequestParam(name = "limit", required = false, defaultValue = "500") Integer limit,
                                   @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                   @RequestParam(name = "timestamp", required = false) Long timestamp) {
        // 最大查询数量 500 默认500
        if (limit == null || limit > MAX_QUERY_TRADES_LIMIT) {
            limit = DEFAULT_QUERY_TRADES_LIMIT;
        }
        try {
            List<WithdrawDetailResult> depositOrderResults = balanceService.queryWithdrawOrder(header, token,
                    fromId, startTime, endTime, limit);
            return ResultUtils.toRestJSONString(depositOrderResults);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query withdrawal orders response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query withdrawal orders Exception: uid:{} startTime:{}, endTime:{}, fromId{}", header.getUserId(), startTime, endTime, fromId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取订单信息
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.OPTION, AccountType.FUTURES})
    @RequestMapping(value = {"/bestOrder", "/v1/bestOrder"})
    public String getBestOrder(Header header,
                               @RequestParam String symbol,
                               @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                               @RequestParam(name = "timestamp", required = false) Long timestamp,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        Integer specialPermission = (Integer) request.getAttribute(OpenApiInterceptor.API_SPECIAL_PERMISSION_ATTR);
        if (!((specialPermission & OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE) == OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "wrong request";
        }
        SymbolResult symbolResult = basicService.querySymbol(header, symbol);
        if (symbolResult == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, PARAM_SYMBOL_NAME);
        }
        Long exchangeId = symbolResult.getExchangeId();
        try {
            BestOrderResult result = orderService.getBestOrder(header, exchangeId, symbol);
            return ResultUtils.toRestJSONString(result);
        } catch (Exception e) {
            log.error("get best order Exception: symbol{}", symbol, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 机构用户空投接口
     *
     * @return
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/user/transfer", "/v1/user/transfer"})
    public String transfer(Header header,
                           HttpServletRequest request,
                           @RequestParam(required = false) Long targetUserId,
                           @RequestParam String clientOrderId,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_AMOUNT)
                           @RequestParam String amount,
                           @RequestParam String tokenId,
                           @RequestParam Integer businessType,
                           @RequestParam(required = false, defaultValue = "0") Integer subBusinessType,
                           @RequestParam(required = false, defaultValue = "") String address,
                           @RequestParam(required = false, defaultValue = "") String addressExt,
                           @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                           @RequestParam(name = "timestamp", required = false) Long timestamp) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        if ((targetUserId == null || targetUserId.equals(0L)) && StringUtils.isEmpty(address)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, TransferConstant.TO_USER_ID);
        }
        if (Strings.isNullOrEmpty(clientOrderId)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, TransferConstant.CLIENT_ORDER_ID);
        }
        if (StringUtils.isEmpty(amount) || new BigDecimal(amount).compareTo(BigDecimal.ZERO) <= 0) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, TransferConstant.AMOUNT);
        }
        if (StringUtils.isEmpty(tokenId)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, TransferConstant.TOKEN_ID);
        }
        try {
            TransferResult result = balanceService.userTransferToUser(header, targetUserId, clientOrderId, amount, tokenId, businessType, subBusinessType, address, addressExt);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case INSUFFICIENT_BALANCE:
                case TRANSFER_INSUFFICIENT_BALANCE:
                    throw new OpenApiException(ApiErrorCode.INSUFFICIENT_BALANCE);
                case PARAM_INVALID:
                    throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER);
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                case FEATURE_NOT_OPEN:
                    throw new OpenApiException(ApiErrorCode.UNAUTHORIZED);
                case ERROR_SUB_BUSINESS_SUBJECT:
                    throw new OpenApiException(ApiErrorCode.ERROR_BUSINESS_SUBJECT);
                case ACCOUNT_NOT_EXIST:
                    throw new OpenApiException(ApiErrorCode.USER_NOT_EXIST);
                case SUCCESS:
                default:
                    log.error("transfer response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("transfer fail targetUserId {},clientOrderId {},amount {},tokenId {},businessType {},e {}", targetUserId, clientOrderId, amount, tokenId, businessType, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/depositAddress", "/v1/depositAddress"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String getDepositAddress(Header header,
                                    HttpServletRequest request,
                                    @RequestParam String tokenId,
                                    @RequestParam(required = false, defaultValue = "") String chainType) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        DepositAddressResult depositResult = balanceService.beforeDeposit(header, tokenId, chainType);
        return JsonUtil.defaultGson().toJson(depositResult);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON}, forceCheckIpWhiteList = true)
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/withdraw", "/v1/withdraw"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String withdraw(Header header,
                           HttpServletRequest request,
                           @RequestParam String clientOrderId,
                           @RequestParam String address,
                           @RequestParam(required = false, defaultValue = "") String addressExt,
                           @RequestParam String tokenId,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_QUANTITY)
                           @RequestParam String withdrawQuantity,
                           @RequestParam(required = false, defaultValue = "") String chainType,
                           @RequestParam(name = "isQuick", required = false, defaultValue = "false") Boolean isQuick,
                           @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                           @RequestParam(name = "timestamp", required = false) Long timestamp) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);

        if (Strings.isNullOrEmpty(clientOrderId)) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, TransferConstant.CLIENT_ORDER_ID);
        }
        if (StringUtils.isEmpty(withdrawQuantity) || new BigDecimal(withdrawQuantity).compareTo(BigDecimal.ZERO) <= 0) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, TransferConstant.AMOUNT);
        }
        if (StringUtils.isEmpty(tokenId)) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, TransferConstant.TOKEN_ID);
        }
        if (StringUtils.isEmpty(address)) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, TransferConstant.ADDRESS);
        }
        try {
            WithdrawResult result = balanceService.openApiWithdraw(header, clientOrderId, address, addressExt, tokenId, withdrawQuantity, chainType, Boolean.TRUE, isQuick);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                case PARAM_INVALID:
                    throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER);
                case ACCOUNT_NOT_EXIST:
                    throw new OpenApiException(ApiErrorCode.USER_NOT_EXIST);
                case WITHDRAW_ADDRESS_ILLEGAL:
                    throw new OpenApiException(ApiErrorCode.WITHDRAW_ADDRESS_ILLEGAL);
                case WITHDRAW_ADDRESS_NOT_IN_WHITE_LIST:
                    throw new OpenApiException(ApiErrorCode.WITHDRAW_ADDRESS_NOT_IN_WHITELIST);
                case REPEATED_SUBMIT_REQUEST:
                    throw new OpenApiException(ApiErrorCode.REPEATED_SUBMIT_REQUEST);
                case UNSUPPORTED_CONTRACT_ADDRESS:
                    throw new OpenApiException(ApiErrorCode.UNSUPPORTED_CONTRACT_ADDRESS);
                case WITHDRAW_FAILED:
                    throw new OpenApiException(ApiErrorCode.WITHDRAW_FAILED);
                case WITHDRAW_AMOUNT_CANNOT_BE_NULL:
                    throw new OpenApiException(ApiErrorCode.WITHDRAW_AMOUNT_CANNOT_BE_NULL);
                case WITHDRAW_AMOUNT_MAX_LIMIT:
                    throw new OpenApiException(ApiErrorCode.WITHDRAW_AMOUNT_MAX_LIMIT);
                case WITHDRAW_AMOUNT_MIN_LIMIT:
                    throw new OpenApiException(ApiErrorCode.WITHDRAW_AMOUNT_MIN_LIMIT);
                case WITHDRAW_AMOUNT_ILLEGAL:
                    throw new OpenApiException(ApiErrorCode.WITHDRAW_AMOUNT_ILLEGAL);
                case INSUFFICIENT_BALANCE:
                    throw new OpenApiException(ApiErrorCode.INSUFFICIENT_BALANCE);
                case SUCCESS:
                default:
                    log.error("transfer response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("withdraw fail clientOrderId {},address {},addressExt {},tokenId {},withdrawQuantity {} chainType {} isQuick {},e {}",
                    clientOrderId, address, addressExt, tokenId, withdrawQuantity, chainType, isQuick, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 2)
    @RequestMapping(value = {"/withdraw/detail", "v1/withdraw/detail"})
    public String withdrawOrderDetail(Header header,
                                      @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                                      @RequestParam(name = "clientOrderId", required = false, defaultValue = "") String clientOrderId) {
        if (StringUtils.isEmpty(clientOrderId) && (orderId == null || orderId.equals(0L))) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, TransferConstant.CLIENT_ORDER_ID);
        }

        try {
            WithdrawDetailResult withdrawDetailResult = balanceService.getWithdrawOrderDetail(header, clientOrderId, orderId);
            return ResultUtils.toRestJSONString(withdrawDetailResult);
        } catch (Exception e) {
            log.error("query withdraw order Exception: orderId {} clientOrderId {} e {}", orderId, clientOrderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/subAccount/query", "/v1//subAccount/query"})
    public String querySubAccount(Header header) {
        List<AccountResult> accountInfoResults = accountService.querySubAccount(header);
        return JsonUtil.defaultGson().toJson(accountInfoResults);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/subAccount/transfer", "/v1/subAccount/transfer", "/transfer", "/v1/transfer"})
    public String subAccountTransfer(Header header,
                                     @RequestParam(required = false, defaultValue = "1") Integer fromAccountType,
                                     @RequestParam(required = false, defaultValue = "0") Integer fromAccountIndex,
                                     @RequestParam(required = false, defaultValue = "1") Integer toAccountType,
                                     @RequestParam(required = false, defaultValue = "0") Integer toAccountIndex,
                                     @RequestParam String tokenId,
                                     @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_AMOUNT)
                                     @RequestParam String amount,
                                     HttpServletRequest request) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        AccountTypeEnum accountTypeEnum = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        if (accountTypeEnum != AccountTypeEnum.COIN && accountIndex > 0) {
            fromAccountType = AccountType.fromAccountTypeEnum(accountTypeEnum).value();
            fromAccountIndex = accountIndex;
            toAccountType = AccountType.MAIN.value();
            toAccountIndex = 0;
        }
        if (fromAccountType.equals(toAccountType) && fromAccountIndex.equals(toAccountIndex)) {
            JsonObject dataObj = new JsonObject();
            dataObj.addProperty("success", Boolean.FALSE);
            return JsonUtil.defaultGson().toJson(dataObj);
        }
        accountService.subAccountTransfer(header, fromAccountType, fromAccountIndex, toAccountType, toAccountIndex, tokenId, amount);
        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("success", Boolean.TRUE);
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/balance_flow", "/v1/balance_flow", "/balanceFlow", "/v1/balanceFlow"})
    public String querySubAccountBalanceFlow(Header header,
                                             @RequestParam(required = false, defaultValue = "1") Integer accountType,
                                             @RequestParam(required = false, defaultValue = "0") Integer accountIndex,
                                             @RequestParam(required = false, defaultValue = "") String tokenId,
                                             @RequestParam(required = false, defaultValue = "") Integer flowType,
                                             @RequestParam(required = false, defaultValue = "0") Long fromFlowId,
                                             @RequestParam(required = false, defaultValue = "0") Long endFlowId,
                                             @RequestParam(required = false, defaultValue = "0") Long startTime,
                                             @RequestParam(required = false, defaultValue = "0") Long endTime,
                                             @RequestParam(required = false, defaultValue = "50") Integer limit,
                                             HttpServletRequest request) {
        AccountTypeEnum apiAccountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer apiAccountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        AccountTypeEnum accountTypeEnum = apiAccountType;
        Integer index = apiAccountIndex;
        if (apiAccountType == AccountTypeEnum.COIN && apiAccountIndex == 0) {
            accountTypeEnum = AccountType.toAccountTypeEnum(accountType);
            index = accountIndex;
        }
        List<BalanceFlowResult> flowList = accountService.queryBalanceFlow(header, tokenId, flowType, fromFlowId, endFlowId, startTime, endTime, limit,
                accountTypeEnum, index);
        return JsonUtil.defaultGson().toJson(flowList);
    }

    @ResponseBody
    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @RequestMapping(value = {"/etf_price", "/v1/etf_price"})
    public String getEtfPriceList(HttpServletRequest request, HttpServletResponse response) {
        Integer specialPermission = (Integer) request.getAttribute(OpenApiInterceptor.API_SPECIAL_PERMISSION_ATTR);
        if (!((specialPermission & OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE) == OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "wrong request";
        }
        List<EtfPriceResult> result = basicService.getEtfPrices();
        return JsonUtil.defaultGson().toJson(result);
    }
}
