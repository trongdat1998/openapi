package io.bhex.openapi.controller;

import com.google.common.base.Strings;
import io.bhex.base.log.LogBizConstants;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.core.validate.HbtcParamType;
import io.bhex.broker.core.validate.ValidHbtcParam;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.*;
import io.bhex.openapi.domain.api.enums.ApiOrderSide;
import io.bhex.openapi.domain.api.enums.ApiOrderType;
import io.bhex.openapi.domain.api.enums.ApiTimeInForce;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.domain.api.result.OptionAccountResult;
import io.bhex.openapi.interceptor.OpenApiInterceptor;
import io.bhex.openapi.interceptor.annotation.LimitAuth;
import io.bhex.openapi.interceptor.annotation.SignAuth;
import io.bhex.openapi.service.AccountService;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.service.OptionOrderService;
import io.bhex.openapi.util.ErrorCodeConvertor;
import io.bhex.openapi.util.ReadOnlyApiKeyCheckUtil;
import io.bhex.openapi.util.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: yuehao  <hao.yue@bhex.com>
 * @CreateDate: 2019-02-14 15:13
 * @Copyright（C）: 2018 BHEX Inc. All rights reserved.
 */
@Slf4j
@Validated
@RestController
@RequestMapping(value = {"/openapi/option"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class OptionOrderController {

    private static final Integer MAX_QUERY_ORDERS_LIMIT = 500;
    private static final Integer DEFAULT_QUERY_TRADES_LIMIT = 500;
    private static final Integer MAX_QUERY_TRADES_LIMIT = 1000;
    private static final String PARAM_SYMBOL_NAME = "symbol";
    private static final String PARAM_QUANTITY_NAME = "quantity";

    @Resource
    private OptionOrderService optionOrderService;

    @Resource
    private BasicService basicService;

    @Autowired
    private AccountService accountService;


    @SignAuth(checkRecvWindow = true, requiredAccountTypes = {AccountType.COMMON, AccountType.OPTION})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT, RateLimitType.ORDERS}, weight = 1)
    @PostMapping(value = {"/order", "/v1/order"})
    public String newOrder(HttpServletRequest request,
                           Header header,
                           @RequestParam(name = "symbol", required = true) String symbol,
                           @RequestParam(name = "side", required = true) String side,
                           @RequestParam(name = "type", required = true) String type,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_QUANTITY)
                           @RequestParam(name = "quantity", required = true) BigDecimal quantity,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_PRICE)
                           @RequestParam(name = "price", required = false, defaultValue = "0") BigDecimal price,
                           @RequestParam(name = "timeInForce", required = false, defaultValue = "GTC") String timeInForce,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.CLIENT_ORDER_ID)
                           @RequestParam(name = "clientOrderId", required = false, defaultValue = "") String clientOrderId,
                           @RequestParam(name = "orderSource", required = false, defaultValue = "") String orderSource) {

        try {
            MDC.put(LogBizConstants.ORDER_ID, clientOrderId);
            ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);

            SymbolResult symbolResult = basicService.querySymbol(header, symbol);
            if (symbolResult == null) {
                throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, PARAM_SYMBOL_NAME);
            }
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

            if (quantity == null || quantity.doubleValue() <= 0) {
                throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, PARAM_QUANTITY_NAME);
            }

            if (Strings.isNullOrEmpty(clientOrderId)) {
                clientOrderId = System.currentTimeMillis() + "" + Thread.currentThread().getId();
            }

            try {
                OrderResult result = optionOrderService.newOptionOrder(header, accountId, accountType, accountIndex,
                        exchangeId, symbol, clientOrderId, side, type, price.toPlainString(), quantity.toPlainString(), timeInForce, orderSource);
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
                        log.error("create option order failed");
                        throw new OpenApiException(ApiErrorCode.DISCONNECTED);
                    case CREATE_ORDER_TIMEOUT:
                        throw new OpenApiException(ApiErrorCode.CREATE_ORDER_TIMEOUT);
                    case OPTION_NOT_EXIST:
                        throw new OpenApiException(ApiErrorCode.OPTION_NOT_EXIST);
                    case OPTION_HAS_EXPIRED:
                        throw new OpenApiException(ApiErrorCode.OPTION_HAS_EXPIRED);
                    case ERR_CANCEL_ORDER_POSITION_LIMIT:
                        throw new OpenApiException(ApiErrorCode.OPTION_ORDER_POSITION_LIMIT);
                    default:
                        log.error("create option order response error:{}", errorCode.toString());
                        throw new OpenApiException(ApiErrorCode.CREATE_ORDER_FAILED);
                }
            } catch (OpenApiException e) {
                throw e;
            } catch (Exception e) {
                log.error(" option order Exception: accountId:{} ,exchangeId{} ,symbol:{} ,cid:{} ,side:{} ,type:{} ,price:{} ,qty:{} ,tif:{}",
                        accountId, exchangeId, symbol, clientOrderId, side, type, price, quantity, timeInForce, e);
                throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            MDC.remove(LogBizConstants.ORDER_ID);
        }
    }

    /**
     * 撤单
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.OPTION})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @DeleteMapping(value = {"/order/cancel", "/v1/order/cancel"})
    public String cancelOrder(HttpServletRequest request,
                              Header header,
                              @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                              @RequestParam(name = "clientOrderId", required = false, defaultValue = "0") String clientOrderId) {

        try {
            MDC.put(LogBizConstants.ORDER_ID, (orderId != null && orderId != 0) ? orderId.toString() : clientOrderId);
            ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
            Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
            AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
            Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
            try {
                OrderResult result = optionOrderService.cancelOptionOrder(header, accountId, accountType, accountIndex, clientOrderId, orderId);
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
                        log.error("cancel order response error:{}", errorCode.toString());
                        throw new OpenApiException(ApiErrorCode.CANCEL_ORDER_FAILED);
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            MDC.remove(LogBizConstants.ORDER_ID);
        }
    }


    /**
     * 获取期权当前委托
     *
     * @param request request
     * @param header  header
     * @param symbol  symbol
     * @param orderId orderId
     * @param limit   limit
     * @param side    side
     * @param type    type
     * @return string
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.OPTION})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/openOrders", "/v1/openOrders"})
    public String queryOpenOrders(HttpServletRequest request,
                                  Header header,
                                  @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol,
                                  @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                                  @RequestParam(name = "limit", required = false) Integer limit,
                                  @RequestParam(name = "side", required = false) String side,
                                  @RequestParam(name = "type", required = false) String type) {
        // 最大查询数量 500 默认500
        if (limit == null || limit <= 0) {
            limit = MAX_QUERY_ORDERS_LIMIT;
        }
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            List<OrderResult> list = optionOrderService.queryCurrentOrders(header, accountId, accountType, accountIndex,
                    symbol, orderId, 0L, 0L, 0L, "", "", type, side, limit);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query option open orders response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query option open orders Exception: accountId:{}, symbol{}, fromOrderId:{}", accountId, symbol, orderId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }


    /**
     * 获取期权历史委托
     *
     * @param request     request
     * @param header      header
     * @param symbol      symbol
     * @param side        side
     * @param type        type
     * @param orderStatus orderStatus
     * @param limit       limit
     * @return string
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.OPTION})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/historyOrders", "/v1/historyOrders"})
    public String queryHistoryOrders(HttpServletRequest request,
                                     Header header,
                                     @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol,
                                     @RequestParam(name = "side", required = false, defaultValue = "") String side,
                                     @RequestParam(name = "type", required = false, defaultValue = "") String type,
                                     @RequestParam(name = "orderStatus", required = false, defaultValue = "") String orderStatus,
                                     @RequestParam(name = "limit", required = false, defaultValue = "500") Integer limit) {
        // 最大查询数量 500 默认500
        if (limit == null || limit <= 0) {
            limit = MAX_QUERY_ORDERS_LIMIT;
        }
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            List<OrderResult> list = optionOrderService.queryHistoryOrders(header, accountId, accountType, accountIndex,
                    symbol, 0L, 0L, 0L, 0L, "", "", type, side, limit, orderStatus);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query option history orders response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query option history orders Exception: accountId:{}, symbol{}", accountId, symbol, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 期权历史成交
     *
     * @param request request
     * @param header  header
     * @param symbol  symbol
     * @param fromId  fromId
     * @param toId    toId
     * @param limit   limit
     * @param side    side
     * @return string
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.OPTION})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/myTrades", "/v1/myTrades"})
    public String myTrades(HttpServletRequest request,
                           Header header,
                           @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol,
                           @RequestParam(name = "fromId", required = false, defaultValue = "0") Long fromId,
                           @RequestParam(name = "toId", required = false, defaultValue = "0") Long toId,
                           @RequestParam(name = "limit", required = false) Integer limit,
                           @RequestParam(name = "side", required = false) String side) {
        // 最大查询数量 500 默认500
        if (limit == null) {
            limit = DEFAULT_QUERY_TRADES_LIMIT;
        }

        if (limit > MAX_QUERY_TRADES_LIMIT) {
            limit = MAX_QUERY_TRADES_LIMIT;
        }
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            List<MatchResult> list = optionOrderService.queryMatchInfo(header, accountId, accountType, accountIndex,
                    symbol, fromId, toId, 0L, 0L, limit, side);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query option trades response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query option trades Exception: accountId:{}, fromId{}", accountId, fromId, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取持仓信息
     *
     * @param request request
     * @param header  header
     * @param symbol  symbol
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.OPTION})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/positions", "/v1/positions"})
    public String queryOptionPositions(HttpServletRequest request,
                                       Header header,
                                       @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol) {

        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            List<PositionResult> list = optionOrderService.getOptionPositions(header, accountId, accountType, accountIndex, symbol, 0, 0L, 0L, 100);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query option position response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query option position Exception: accountId:{}, symbol{}", accountId, symbol, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取期权交割历史
     *
     * @param request request
     * @param header  header
     * @param symbol  symbol
     */
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.OPTION})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/settlements", "/v1/settlements"})
    public String queryOptionSettlements(HttpServletRequest request,
                                         Header header,
                                         @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol) {
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            List<SettlementResult> list = optionOrderService.getOptionSettlement(header, accountId, accountType, accountIndex,
                    symbol, 0L, 0L, 0L, 0L, 100);
            return ResultUtils.toRestJSONString(list);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                default:
                    log.error("request query option settlement response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.DISCONNECTED);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("query option settlement Exception: accountId:{}, symbol{}", accountId, symbol, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    /**
     * 获取期权账户信息
     */
    // TODO
    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.OPTION})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/account", "/v1/account"})
    public String optionAccountInfo(HttpServletRequest request,
                                    Header header,
                                    @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                    @RequestParam(name = "timestamp", required = false) Long timestamp) {
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            OptionAccountResult result = accountService.getOptionAccount(header, accountId, accountType, accountIndex);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case GRPC_SERVER_TIMEOUT:
                    log.error("request account info timeout");
                    throw new OpenApiException(ApiErrorCode.TIMEOUT);
                case GRPC_SERVER_SYSTEM_ERROR:
                    log.error("request account info has system error");
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

    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/getOrder", "/v1/getOrder"})
    public String getOrder(HttpServletRequest request, Header header,
                           @RequestParam(name = "orderId", required = false) Long orderId,
                           @RequestParam(name = "clientOrderId", required = false) String clientOrderId,
                           @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                           @RequestParam(name = "timestamp", required = false) Long timestamp) {
        // orderId和clientOrderId二者必选其一
        if ((orderId == null || orderId <= 0) && StringUtils.isEmpty(clientOrderId)) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, "orderId or clientOrderId");
        }

        try {
            Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
            OrderResult result = optionOrderService.getOptionOrder(header, accountId, clientOrderId, orderId);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }
}
