package io.bhex.openapi.controller;

import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.core.validate.HbtcParamType;
import io.bhex.broker.core.validate.ValidHbtcParam;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.order.OrderQueryType;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.AccountType;
import io.bhex.openapi.domain.SymbolResult;
import io.bhex.openapi.domain.api.enums.ApiOrderSide;
import io.bhex.openapi.domain.api.enums.ApiOrderType;
import io.bhex.openapi.domain.api.enums.ApiTimeInForce;
import io.bhex.openapi.domain.api.result.AccountResult;
import io.bhex.openapi.domain.api.result.CancelOrderResult;
import io.bhex.openapi.domain.api.result.NewOrderResult;
import io.bhex.openapi.domain.api.result.QueryOrderResult;
import io.bhex.openapi.domain.finance_support.FinanceAccountResult;
import io.bhex.openapi.interceptor.annotation.FinanceSupportAuth;
import io.bhex.openapi.interceptor.annotation.SignAuth;
import io.bhex.openapi.service.AccountService;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.service.FinanceSupportService;
import io.bhex.openapi.service.OrderService;
import io.bhex.openapi.util.ErrorCodeConvertor;
import io.bhex.openapi.util.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = {"/openapi/finance_support", "/openapi/finance_support/v1"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class FinanceSupportController {

    private static final Long GLOBAL_FINANCE_INTERAL_USERID = 10000L;

    @Resource
    private BasicService basicService;

    @Resource
    private FinanceSupportService financeSupportService;

    @Resource
    private OrderService orderService;

    @Resource
    private AccountService accountService;

    @SignAuth(checkRecvWindow = true, requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @FinanceSupportAuth
    @PostMapping(value = {"/order"})
    public String newOrder(Header originHeader,
                           @RequestParam(name = "symbol") String symbol,
                           @RequestParam(name = "orgId", required = false, defaultValue = "0") Long orgId,
                           @RequestParam(name = "accountId", required = false, defaultValue = "0") Long accountId,
                           @RequestParam(name = "side") String side,
                           @RequestParam(name = "type") String type,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_QUANTITY)
                           @RequestParam(name = "quantity", required = false, defaultValue = "0") BigDecimal quantity,
                           @RequestParam(name = "timeInForce", required = false, defaultValue = "GTC") String timeInForce,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_PRICE)
                           @RequestParam(name = "price", required = false, defaultValue = "0") BigDecimal price,
                           @RequestParam(name = "clientOrderId") String clientOrderId) {
        FinanceAccountResult accountInfo;
        if (orgId.equals(6002L)) {
            // 财务账户目前都在6002 针对财务账户需要下单做特殊处理
            if (accountId == null || accountId <= 0) {
                throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "accountId");
            }
            accountInfo = financeSupportService.getFinanceAccount(null, accountId);
        } else {
            checkOrgIdAndAccountId(orgId, accountId);
            accountInfo = financeSupportService.getFinanceAccount(orgId, accountId);
        }
        Header header = newHeader(originHeader, accountInfo.getUserId(), orgId);

        SymbolResult symbolResult = basicService.querySymbol(header, symbol);
        if (symbolResult == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "symbol");
        }
        Long exchangeId = symbolResult.getExchangeId();

        ApiOrderType apiOrderType = ApiOrderType.fromValue(type);
        if (apiOrderType == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_ORDER_TYPE);
        }

        ApiOrderSide apiOrderSide = ApiOrderSide.fromValue(side.toLowerCase());
        if (apiOrderSide == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_SIDE);
        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, "quantity");
        }

        ApiTimeInForce timeInForceEnum = ApiTimeInForce.formValue(timeInForce.toUpperCase());
        if (timeInForceEnum == null) {
            throw new OpenApiException(ApiErrorCode.INVALID_TIF);
        }

        try {
            NewOrderResult result = orderService.newOrder(header, accountId,
                    AccountTypeEnum.forNumber(accountInfo.getAccountType()), accountInfo.getAccountIndex(),
                    exchangeId, symbol, clientOrderId, side, type,
                    price.toString(), quantity.toString(), timeInForceEnum, "", "");
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @FinanceSupportAuth
    @GetMapping(value = {"/order"})
    public String queryOrder(Header originHeader,
                             @RequestParam(name = "accountId", required = false, defaultValue = "0") Long accountId,
                             @RequestParam(name = "orgId", required = false, defaultValue = "0") Long orgId,
                             @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                             @RequestParam(name = "clientOrderId", required = false, defaultValue = "0") String clientOrderId) {
        checkOrgIdAndAccountId(orgId, accountId);

        FinanceAccountResult accountInfo = financeSupportService.getFinanceAccount(orgId, accountId);
        Header header = newHeader(originHeader, accountInfo.getUserId(), orgId);
        try {
            QueryOrderResult result = orderService.getOrder(header, accountId,
                    AccountTypeEnum.forNumber(accountInfo.getAccountType()), accountInfo.getAccountIndex(),
                    clientOrderId, orderId);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @FinanceSupportAuth
    @DeleteMapping(value = {"/order"})
    public String cancelOrder(Header originHeader,
                              @RequestParam(name = "accountId", required = false, defaultValue = "0") Long accountId,
                              @RequestParam(name = "orgId", required = false, defaultValue = "0") Long orgId,
                              @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                              @RequestParam(name = "clientOrderId", required = false, defaultValue = "0") String clientOrderId) {
        checkOrgIdAndAccountId(orgId, accountId);

        try {
            FinanceAccountResult accountInfo = financeSupportService.getFinanceAccount(orgId, accountId);
            Header header = newHeader(originHeader, accountInfo.getUserId(), orgId);
            CancelOrderResult result = orderService.cancelOrder(header, accountId,
                    AccountTypeEnum.forNumber(accountInfo.getAccountType()), accountInfo.getAccountIndex(),
                    clientOrderId, orderId, "");
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @FinanceSupportAuth
    @RequestMapping(value = {"/openOrders"})
    public String openOrders(Header originHeader,
                             @RequestParam(name = "accountId", required = false, defaultValue = "0") Long accountId,
                             @RequestParam(name = "orgId", required = false, defaultValue = "0") Long orgId,
                             @RequestParam(name = "symbol", required = false, defaultValue = "") String symbol,
                             @RequestParam(name = "orderId", required = false, defaultValue = "0") Long orderId,
                             @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit) {
        checkOrgIdAndAccountId(orgId, accountId);

        try {
            FinanceAccountResult accountInfo = financeSupportService.getFinanceAccount(orgId, accountId);
            Header header = newHeader(originHeader, accountInfo.getUserId(), orgId);
            List<QueryOrderResult> result = orderService.queryOrders(header, OrderQueryType.CURRENT, accountId,
                    AccountTypeEnum.forNumber(accountInfo.getAccountType()), accountInfo.getAccountIndex(),
                    symbol, "", orderId, 0L, 0L, 0L, limit);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @FinanceSupportAuth
    @PostMapping(value = {"/account"})
    public String createAccount(@RequestParam(name = "orgId", required = false, defaultValue = "0") Long orgId) {

        if (orgId == null || orgId <= 0) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "orgId");
        }

        try {
            FinanceAccountResult result = financeSupportService.createFinanceAccount(GLOBAL_FINANCE_INTERAL_USERID, orgId);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @FinanceSupportAuth
    @RequestMapping(value = {"/account"})
    public String getAccountInfo(Header originHeader,
                                 @RequestParam(name = "accountId", required = false, defaultValue = "0") Long accountId,
                                 @RequestParam(name = "orgId", required = false, defaultValue = "0") Long orgId) {
        checkOrgIdAndAccountId(orgId, accountId);

        try {
            FinanceAccountResult accountInfo = financeSupportService.getFinanceAccount(orgId, accountId);
            Header header = newHeader(originHeader, accountInfo.getUserId(), orgId);
            if (accountInfo.getCreateType() == 1) {
                // 目前手动创建的账户都是财务账户，并且都是6002下的账户，所以这里查询的orgId强制设置为6002
                header = newHeader(originHeader, accountInfo.getUserId(), 6002L);
            }
            AccountResult result = accountService.getAccount(header, accountId,
                    AccountTypeEnum.forNumber(accountInfo.getAccountType()), accountInfo.getAccountIndex());
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @FinanceSupportAuth
    @RequestMapping(value = {"/accounts"})
    public String getAllFinanceAccounts() {
        List<FinanceAccountResult> results = financeSupportService.getAllFinanceAccounts();
        return ResultUtils.toRestJSONString(results);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN})
    @FinanceSupportAuth
    @PostMapping(value = {"transfer"})
    public String transfer(@RequestParam(name = "sourceAccountId", required = false, defaultValue = "0") Long sourceAccountId,
                           @RequestParam(name = "sourceOrgId", required = false, defaultValue = "0") Long sourceOrgId,
                           @RequestParam(name = "targetAccountId", required = false, defaultValue = "0") Long targetAccountId,
                           @RequestParam(name = "targetOrgId", required = false, defaultValue = "0") Long targeOrgId,
                           @ValidHbtcParam(message = "30003", type = HbtcParamType.ORDER_AMOUNT)
                           @RequestParam(name = "amount", required = false, defaultValue = "0") String amount,
                           @RequestParam(name = "tokenId") String tokenId) {
        checkOrgIdAndAccountId(sourceOrgId, sourceAccountId);
        checkOrgIdAndAccountId(targeOrgId, targetAccountId);

        Long clientTransferId = System.currentTimeMillis();
        log.info("FinanceAccountTransfer sourceAccountId: {} sourceOrgId: {} targetAccountId: {} " +
                        "targeOrgId: {} amount: {} tokenId: {} clientTransferId: {}",
                sourceAccountId, sourceOrgId, targetAccountId, targeOrgId, amount, tokenId, clientTransferId);
        try {
            financeSupportService.transfer(sourceAccountId, sourceOrgId, targetAccountId,
                    targeOrgId, amount, tokenId, clientTransferId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return ResultUtils.toRestJSONString(result);
        } catch (BrokerException e) {
            ApiErrorCode errorCode = ErrorCodeConvertor.convert(
                    BrokerErrorCode.fromCode(e.getCode()), ApiErrorCode.UNKNOWN);
            throw new OpenApiException(errorCode);
        }
    }

    private Header newHeader(Header originHeader, Long userId, Long orgId) {
        return originHeader.toBuilder().userId(userId).orgId(orgId).build();
    }

    private void checkOrgIdAndAccountId(Long orgId, Long accountId) {
        if (orgId == null || orgId <= 0) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "orgId");
        }

        if (accountId == null || accountId <= 0 || !financeSupportService.isValidFinanceAccount(orgId, accountId)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "accountId");
        }
    }
}
