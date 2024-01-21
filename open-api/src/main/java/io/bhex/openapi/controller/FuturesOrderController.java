package io.bhex.openapi.controller;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.bhex.base.log.LogBizConstants;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.core.validate.HbtcParamType;
import io.bhex.broker.core.validate.ValidHbtcParam;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.common.OpenApiSpecialPermission;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.AccountType;
import io.bhex.openapi.domain.BestOrderResult;
import io.bhex.openapi.domain.DepthInfoResult;
import io.bhex.openapi.domain.FuturesMatchResult;
import io.bhex.openapi.domain.FuturesOrderResult;
import io.bhex.openapi.domain.FuturesPositionResult;
import io.bhex.openapi.domain.SymbolResult;
import io.bhex.openapi.domain.api.enums.ApiFuturesOrderSide;
import io.bhex.openapi.domain.api.enums.ApiFuturesOrderType;
import io.bhex.openapi.domain.api.enums.ApiFuturesPositionSide;
import io.bhex.openapi.domain.api.enums.ApiFuturesPriceType;
import io.bhex.openapi.domain.api.enums.ApiFuturesTimeInForce;
import io.bhex.openapi.domain.api.enums.ApiOrderSide;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.domain.api.result.FastCancelOrderResult;
import io.bhex.openapi.domain.api.result.FundingRateResult;
import io.bhex.openapi.domain.api.result.FuturesAccountResult;
import io.bhex.openapi.domain.api.result.HistoryFundingRateResult;
import io.bhex.openapi.domain.api.result.InsuranceFundResult;
import io.bhex.openapi.domain.api.result.ModifyMarginResult;
import io.bhex.openapi.interceptor.OpenApiInterceptor;
import io.bhex.openapi.interceptor.annotation.LimitAuth;
import io.bhex.openapi.interceptor.annotation.SignAuth;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.service.FuturesOrderService;
import io.bhex.openapi.service.OrderService;
import io.bhex.openapi.util.ErrorCodeConvertor;
import io.bhex.openapi.util.ReadOnlyApiKeyCheckUtil;
import io.bhex.openapi.util.ResultUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping(value = {"/openapi/contract", "/openapi/contract/v1"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class FuturesOrderController {

    public static final BigDecimal MIN_TRANSFER_MARGIN_AMOUNT = new BigDecimal("0.000000000000000001");

    private static final String PARAM_SYMBOL = "symbol";

    @Resource
    private FuturesOrderService futuresOrderService;

    @Resource
    private BasicService basicService;

    @Resource
    private OrderService orderService;

    @SignAuth(checkRecvWindow = true, requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT, RateLimitType.ORDERS}, weight = 1)
    @PostMapping(value = {"/order"})
    public String newOrder(HttpServletRequest request, Header header,
                           @RequestParam(name = "symbol") String symbolId,
                           @RequestParam(name = "side") String orderSide,
                           @RequestParam(name = "orderType") String orderType,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_QUANTITY)
                           @RequestParam(name = "quantity") BigDecimal quantity,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_PRICE)
                           @RequestParam(name = "price", required = false, defaultValue = "0") BigDecimal price,
                           @RequestParam(name = "priceType", required = false, defaultValue = "INPUT") String priceType,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_PRICE)
                           @RequestParam(name = "triggerPrice", required = false) BigDecimal triggerPrice,
                           @RequestParam(name = "leverage", required = false) BigDecimal leverage,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_PRICE)
                           @RequestParam(name = "overPrice", required = false) BigDecimal overPrice,
                           @RequestParam(name = "timeInForce", required = false, defaultValue = "GTC") String timeInForce,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.CLIENT_ORDER_ID)
                           @RequestParam(name = "clientOrderId") String clientOrderId,
                           @RequestParam(name = "methodVersion", required = false, defaultValue = "") String methodVersion,
                           @RequestParam(name = "orderSource", required = false, defaultValue = "") String orderSource) {
        try {
            MDC.put(LogBizConstants.ORDER_ID, clientOrderId);
            ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);

            Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
            AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
            Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
            Long exchangeId = getExchangeId(header, symbolId);

            // 请求参数检查
            checkEnumParam(ApiFuturesOrderType.class, orderType, ApiErrorCode.INVALID_ORDER_TYPE);
            checkEnumParam(ApiFuturesOrderSide.class, orderSide, ApiErrorCode.INVALID_SIDE);
            checkEnumParam(ApiFuturesPriceType.class, priceType, ApiErrorCode.INVALID_PRICE_TYPE);
            checkEnumParam(ApiFuturesTimeInForce.class, timeInForce, ApiErrorCode.INVALID_TIF);

            checkBigDecimalParam(quantity, "quantity");

            if (leverage != null) {
                checkBigDecimalParam(leverage, "leverage");
            }

            if (orderType.equals(ApiFuturesOrderType.STOP.name())) {
                checkBigDecimalParam(triggerPrice, "triggerPrice");
            }

            if (priceType.equals(ApiFuturesPriceType.INPUT.name())) {
                checkBigDecimalParam(price, "price");
            }

            try {
                String priceVal = (price == null) ? "" : price.toPlainString();
                String triggerPriceVal = (triggerPrice == null) ? "" : triggerPrice.toPlainString();

                FuturesOrderResult result = futuresOrderService.newFuturesOrder(
                        header, exchangeId, symbolId, accountId, accountType, accountIndex,
                        clientOrderId, orderSide, orderType, priceVal, triggerPriceVal,
                        priceType, quantity.toPlainString(), timeInForce,
                        leverage == null ? "" : leverage.toPlainString(), methodVersion, orderSource);
                return ResultUtils.toRestJSONString(result);
            } catch (BrokerException e) {
                ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                        BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.CREATE_ORDER_FAILED);
                throw new OpenApiException(errorCode);
            }
        } finally {
            MDC.remove(LogBizConstants.ORDER_ID);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @DeleteMapping(value = {"/order/cancel"})
    public String cancelOrder(HttpServletRequest request, Header header,
                              @RequestParam(name = "orderId", required = false) Long orderId,
                              @RequestParam(name = "clientOrderId", required = false) String clientOrderId,
                              @RequestParam(name = "orderType", required = false, defaultValue = "LIMIT") String orderType,
                              @RequestParam(name = "methodVersion", required = false, defaultValue = "") String methodVersion,
                              @RequestParam(name = "fastCancel", required = false, defaultValue = "0") Integer fastCancel,
                              @RequestParam(name = "symbolId", required = false, defaultValue = "") String symbolId,
                              @RequestParam(name = "securityType", required = false, defaultValue = "3") Integer securityType) {
        try {
            MDC.put(LogBizConstants.ORDER_ID, String.format("%s_%s", clientOrderId, orderId));
            ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);

            Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
            AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
            Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
            checkEnumParam(ApiFuturesOrderType.class, orderType, ApiErrorCode.INVALID_ORDER_TYPE);

            // orderId和clientOrderId二者必选其一
            if ((orderId == null || orderId <= 0) && StringUtils.isEmpty(clientOrderId)) {
                throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, "orderId or clientOrderId");
            }

            try {
                if (fastCancel >= 1 && StringUtils.isNotEmpty(symbolId)) {
                    FastCancelOrderResult result = orderService.fastCancelOrder(header, accountId, clientOrderId, orderId, symbolId, securityType, accountType, accountIndex);
                    return ResultUtils.toRestJSONString(result);
                }
                FuturesOrderResult result = futuresOrderService.cancelFuturesOrder(header, accountId, accountType, accountIndex, orderId, clientOrderId, orderType, methodVersion);
                return ResultUtils.toRestJSONString(result);
            } catch (BrokerException e) {
                ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                        BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.CANCEL_ORDER_FAILED);
                throw new OpenApiException(errorCode);
            }
        } finally {
            MDC.remove(LogBizConstants.ORDER_ID);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @DeleteMapping(value = {"/order/batchCancel"})
    public String batchCancelOrder(HttpServletRequest request, Header header,
                                   @RequestParam(name = "symbol", required = false) String symbolIds,
                                   @RequestParam(name = "side", required = false) String orderSide) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        List<String> symbolIdList = split(symbolIds, ",");
        if (symbolIdList.isEmpty()) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, "symbol, orderIds or clientOrderIds");
        }

        if (StringUtils.isNotEmpty(orderSide)) {
            checkEnumParam(ApiOrderSide.class, orderSide, ApiErrorCode.INVALID_SIDE);
        }
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        try {
            futuresOrderService.batchCancelFuturesOrder(header, accountId, accountType, accountIndex, symbolIdList, orderSide);
//            if (!symbolIdList.isEmpty()) {
//            } else {
//                futuresOrderService.batchCancelFuturesOrder(header, orderIdList, clientOrderIdList);
//            }
            JsonObject data = new JsonObject();
            data.addProperty("message", "success");
            data.addProperty("timestamp", System.currentTimeMillis());
            return ResultUtils.toRestJSONString(data);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.CANCEL_ORDER_FAILED);
            throw new OpenApiException(errorCode);
        }

    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/getOrder"})
    public String getOrder(HttpServletRequest request, Header header,
                           @RequestParam(name = "orderId", required = false) Long orderId,
                           @RequestParam(name = "clientOrderId", required = false) String clientOrderId,
                           @RequestParam(name = "orderType", required = false, defaultValue = "LIMIT") String orderType) {
        checkEnumParam(ApiFuturesOrderType.class, orderType, ApiErrorCode.INVALID_ORDER_TYPE);

        // orderId和clientOrderId二者必选其一
        if ((orderId == null || orderId <= 0) && StringUtils.isEmpty(clientOrderId)) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, "orderId or clientOrderId");
        }

        try {
            Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
            AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
            Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);

            FuturesOrderResult result = futuresOrderService.getOrder(header, accountId, accountType, accountIndex, orderId, clientOrderId, orderType);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.OPTION, AccountType.FUTURES})
    @RequestMapping(value = {"/getBestOrder"})
    public String getBestOrder(HttpServletRequest request, HttpServletResponse response, Header header,
                               @RequestParam(name = "symbol") String symbolId) {
        Integer specialPermission = (Integer) request.getAttribute(OpenApiInterceptor.API_SPECIAL_PERMISSION_ATTR);
        if (!((specialPermission & OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE) == OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "wrong request";
        }

        checkSymbol(header, symbolId);
        Long exchangeId = getExchangeId(header, symbolId);
        try {
            BestOrderResult result = futuresOrderService.getBestOrder(header, symbolId, exchangeId);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.OPTION, AccountType.FUTURES})
    @RequestMapping(value = {"/depthInfo", "/v1/depthInfo"})
    public String getDepthInfo(Header header,
                               @RequestParam String symbols,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        Integer specialPermission = (Integer) request.getAttribute(OpenApiInterceptor.API_SPECIAL_PERMISSION_ATTR);
        if (!((specialPermission & OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE) == OpenApiSpecialPermission.CAN_INVOKE_BEST_ORDERS_VALUE)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "wrong request";
        }
        Long exchangeId = getExchangeId(header, symbols.split(",")[0]);
        try {
            DepthInfoResult result = futuresOrderService.getDepthInfo(header, exchangeId, symbols);
            return ResultUtils.toRestJSONString(result);
        } catch (Exception e) {
            log.error("get depth info Exception: symbol{}", symbols, e);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/openOrders"})
    public String openOrders(HttpServletRequest request, Header header,
                             @RequestParam(name = "symbol", required = false, defaultValue = "") String symbolId,
                             @RequestParam(name = "orderId", required = false) Long orderId,
                             @RequestParam(name = "orderType", required = false, defaultValue = "LIMIT") String orderType,
                             @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit) {
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        return queryFuturesOrders(header, accountId, accountType, accountIndex, orderType, orderId, limit, symbolId, false);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/historyOrders"})
    public String historyOrders(HttpServletRequest request, Header header,
                                @RequestParam(name = "symbol", required = false, defaultValue = "") String symbolId,
                                @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                                @RequestParam(name = "orderType", required = false, defaultValue = "LIMIT") String orderType,
                                @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit) {
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        return queryFuturesOrders(header, accountId, accountType, accountIndex, orderType, orderId, limit, symbolId, true);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/myTrades"})
    public String myTrades(HttpServletRequest request, Header header,
                           @RequestParam(name = "symbol", required = false, defaultValue = "") String symbolId,
                           @RequestParam(name = "fromId", required = false, defaultValue = "0") Long fromId,
                           @RequestParam(name = "toId", required = false, defaultValue = "0") Long toId,
                           @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit) {
//        if (StringUtils.isNotEmpty(orderSide)) {
//            checkEnumParam(ApiFuturesOrderSide.class, orderSide, ApiErrorCode.INVALID_SIDE);
//        }

        if (StringUtils.isNotEmpty(symbolId)) {
            checkSymbol(header, symbolId);
        }
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        List<FuturesMatchResult> results = futuresOrderService.queryFuturesMatchInfo(
                header, accountId, accountType, accountIndex, symbolId, fromId, toId, limit);
        return ResultUtils.toRestJSONString(results);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/positions"})
    public String positions(HttpServletRequest request, Header header,
                            @RequestParam(name = "symbol", required = false, defaultValue = "") String symbolId,
                            @RequestParam(name = "side", required = false) String positionSide) {
        List<String> symbolIdList = new ArrayList<>();
        if (StringUtils.isNotEmpty(symbolId)) {
            checkSymbol(header, symbolId);
            symbolIdList.add(symbolId);
        }

        if (StringUtils.isNotEmpty(positionSide)) {
            checkEnumParam(ApiFuturesPositionSide.class, positionSide, ApiErrorCode.INVALID_POSITION_SIDE);
        }
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        List<FuturesPositionResult> results = futuresOrderService.getFuturesPositions(header, accountId, accountType, accountIndex, symbolIdList, positionSide);
        return ResultUtils.toRestJSONString(results);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 5)
    @RequestMapping(value = {"/account"})
    public String getAccountInfo(HttpServletRequest request, Header header) {
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        Map<String, FuturesAccountResult> resultMap = futuresOrderService.getAccountInfo(header, accountId, accountType, accountIndex);
        return ResultUtils.toRestJSONString(resultMap);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @PostMapping(value = {"/modifyMargin"})
    public String modifyMargin(HttpServletRequest request, Header header,
                               @RequestParam(name = "symbol") String symbolId,
                               @RequestParam(name = "side") String positionSide,
                               @RequestParam(name = "amount") BigDecimal amount) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);

        checkSymbol(header, symbolId);
        checkEnumParam(ApiFuturesPositionSide.class, positionSide, ApiErrorCode.INVALID_POSITION_SIDE);
        checkBigDecimalParam(amount, "amount");
        if (amount.abs().compareTo(MIN_TRANSFER_MARGIN_AMOUNT) <= 0) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "amount");
        }
        try {
            Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
            AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
            Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
            ModifyMarginResult result = futuresOrderService.modifyMargin(header, accountId, accountType, accountIndex, symbolId, positionSide, amount);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @RequestMapping(value = {"/insurance"})
    public String getInsuranceFunds(Header header,
                                    @RequestParam(name = "symbol", required = false, defaultValue = "") String symbolId,
                                    @RequestParam(name = "fromId", required = false, defaultValue = "0") Long fromId,
                                    @RequestParam(name = "endId", required = false, defaultValue = "0") Long endId,
                                    @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit) {
        if (StringUtils.isNotEmpty(symbolId)) {
            checkSymbol(header, symbolId);
        }

        if (fromId < 0 || endId < 0) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "fromId or endId");
        }

        if (limit < 0) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "limit");
        }
        List<InsuranceFundResult> results = futuresOrderService.getInsuranceFunds(header, symbolId, fromId, endId, limit);
        return ResultUtils.toRestJSONString(results);
    }

    @RequestMapping(value = {"/fundingRate"})
    public String getFundingRates(Header header,
                                  @RequestParam(name = "symbol", required = false, defaultValue = "") String symbolId,
                                  @RequestParam(name = "state", required = false, defaultValue = "current") String state) {

        // TODO: 当前只支持最新的一次结算周期
        if (!"current".equals(state)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "state");
        }

        if (StringUtils.isNotEmpty(symbolId)) {
            checkSymbol(header, symbolId);
        }

        List<FundingRateResult> results = futuresOrderService.getFundingRates(header, symbolId);
        return ResultUtils.toRestJSONString(results);
    }

    @RequestMapping(value = {"/historyFundingRate"})
    public String getHistoryFundingRates(Header header,
                                         @RequestParam(name = "symbol", required = false, defaultValue = "") String symbolId,
                                         @RequestParam(name = "fromId", required = false, defaultValue = "0") Long fromId,
                                         @RequestParam(name = "endId", required = false, defaultValue = "0") Long endId,
                                         @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit) {
        if (StringUtils.isNotEmpty(symbolId)) {
            checkSymbol(header, symbolId);
        }

        List<HistoryFundingRateResult> results = futuresOrderService.getHistoryFundingRates(header, symbolId, fromId, endId, limit);
        return ResultUtils.toRestJSONString(results);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/setRiskLimit"})
    public String setRiskLimit(HttpServletRequest request, Header header, HttpServletResponse response,
                               @RequestParam(name = "symbol") String symbolId,
                               @RequestParam(name = "riskLimitId") Long riskLimitId,
                               @RequestParam(name = "isLong") Boolean isLong) {
//        Integer specialPermission = (Integer) request.getAttribute(OpenApiInterceptor.API_SPECIAL_PERMISSION_ATTR);
//        if (!((specialPermission & OpenApiSpecialPermission.CAN_SET_FUTURES_RISK_LIMIT_VALUE) == OpenApiSpecialPermission.CAN_SET_FUTURES_RISK_LIMIT_VALUE)) {
//            response.setStatus(HttpStatus.NOT_FOUND.value());
//            return "wrong request";
//        }
        checkSymbol(header, symbolId);

        try {
            AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
            Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
            Map<String, Object> result = futuresOrderService.setRiskLimit(header, symbolId, isLong, riskLimitId, accountType, accountIndex);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.FUTURES})
    @RequestMapping(value = {"/pullPositions"})
    public String marketPullPositions(HttpServletRequest request, Header header, HttpServletResponse response,
                                      @RequestParam(name = "symbol") String symbolId,
                                      @RequestParam(name = "limit", required = false, defaultValue = "20") Long limit) {
        Integer specialPermission = (Integer) request.getAttribute(OpenApiInterceptor.API_SPECIAL_PERMISSION_ATTR);
        if (!((specialPermission & OpenApiSpecialPermission.CAN_PULL_FUTURES_POSITIONS_VALUE) == OpenApiSpecialPermission.CAN_PULL_FUTURES_POSITIONS_VALUE)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "wrong request";
        }

        if (limit <= 0 || limit > 100) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "limit");
        }

        checkSymbol(header, symbolId);

        try {
            List<FuturesPositionResult> results = futuresOrderService.marketPullPositions(header, symbolId, limit);
            return ResultUtils.toRestJSONString(results);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    private String queryFuturesOrders(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex,
                                      String orderType, Long orderId,
                                      Integer limit, String symbolId, boolean isHisorty) {
//        if (StringUtils.isNotEmpty(orderSide)) {
//            checkEnumParam(ApiFuturesOrderSide.class, orderSide, ApiErrorCode.INVALID_SIDE);
//        }

        checkEnumParam(ApiFuturesOrderType.class, orderType, ApiErrorCode.INVALID_ORDER_TYPE);

        if (orderId != null && orderId < 0) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "orderId");
        }

        if (limit < 0) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "limit");
        }

        if (StringUtils.isNotEmpty(symbolId)) {
            checkSymbol(header, symbolId);
        }

        try {
            List<FuturesOrderResult> results = futuresOrderService.queryFuturesOrders(
                    header, accountId, accountType, accountIndex, orderType, symbolId, orderId, limit, isHisorty);
            return ResultUtils.toRestJSONString(results);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.ERR_UPSTREAM_BUSINESS);
            throw new OpenApiException(errorCode);
        }
    }

    private Long getExchangeId(Header header, String symbolId) {
        return checkSymbol(header, symbolId).getExchangeId();
    }

    private SymbolResult checkSymbol(Header header, String symbolId) {
        SymbolResult symbolResult = basicService.querySymbol(header, symbolId);
        if (symbolResult == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, PARAM_SYMBOL);
        } else {
//            if (symbolResult.getForbidOpenapiTrade()) {
//                throw new OpenApiException(ApiErrorCode.SYMBOL_PROHIBIT_ORDER);
//            }
            return symbolResult;
        }
    }

    @SuppressWarnings("unchecked")
    private void checkEnumParam(Class enumClass, String value, ApiErrorCode apiErrorCode) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("invalid enum class type.");
        }

        try {
            Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new OpenApiException(apiErrorCode);
        }
    }

    private void checkBigDecimalParam(BigDecimal paramValue, String paramName) {
        if (paramValue == null || paramValue.compareTo(BigDecimal.ZERO) == 0) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, paramName);
        }
    }

    private List<String> split(String paramValue, String sep) {
        String[] strings = StringUtils.split(paramValue, sep);
        return strings == null ? Collections.emptyList() : Arrays.asList(strings);
    }

    private List<Long> splitAsLong(String paramName, String paramValue, String sep) {
        List<String> strList = split(paramValue, sep);

        try {
            return strList.stream().map(Long::valueOf).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, paramName);
        }
    }
}
