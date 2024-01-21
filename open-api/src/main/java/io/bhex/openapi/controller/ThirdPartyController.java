package io.bhex.openapi.controller;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.entity.RequestPlatform;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.util.HeaderUtil;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.broker.core.validate.HbtcParamType;
import io.bhex.broker.core.validate.ValidHbtcParam;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.AccountType;
import io.bhex.openapi.domain.ApiKeyResult;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.domain.api.result.BalanceResult;
import io.bhex.openapi.domain.api.result.ThirdPartyUserTokenResult;
import io.bhex.openapi.interceptor.annotation.LimitAuth;
import io.bhex.openapi.interceptor.annotation.SignAuth;
import io.bhex.openapi.service.AccountService;
import io.bhex.openapi.service.UserSecurityService;
import io.bhex.openapi.util.ReadOnlyApiKeyCheckUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = {"/openapi/third/user", "/openapi/v1/third/user"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ThirdPartyController {

    @Resource
    private AccountService accountService;

    @Resource
    private UserSecurityService userSecurityService;

    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/create"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String createThirdPartyAccount(Header header, HttpServletRequest request, @RequestParam String thirdUserId) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        if (StringUtils.isEmpty(thirdUserId)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER);
        }
        try {
            JsonObject resultJson = new JsonObject();
            Long userId = accountService.createThirdPartyAccount(header, thirdUserId);
            resultJson.addProperty("userId", userId.toString());
            return JsonUtil.defaultGson().toJson(resultJson);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case THIRD_TOKEN_EXIST:
                    throw new OpenApiException(ApiErrorCode.THIRD_TOKEN_EXIST);
                case NO_PERMISSION_TO_CREATE_VIRTUAL_ACCOUNT:
                    throw new OpenApiException(ApiErrorCode.NO_PERMISSION_TO_CREATE_VIRTUAL_ACCOUNT);
                case THIRD_USER_EXIST:
                    throw new OpenApiException(ApiErrorCode.THIRD_USER_EXIST);
                case THIRD_USER_NOT_EXIST:
                    throw new OpenApiException(ApiErrorCode.THIRD_USER_NOT_EXIST);
                case BIND_THIRD_USER_ERROR:
                    throw new OpenApiException(ApiErrorCode.BIND_THIRD_USER_ERROR);
                default:
                    log.error("create third party user error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.BIND_THIRD_USER_ERROR);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Create third party user error e {} orgId {} thirdUserId {}", e, header.getOrgId(), thirdUserId);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON}, forceCheckIpWhiteList = true)
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/login"})
    public String createThirdPartyToken(Header header,
                                        @RequestParam(required = false, defaultValue = "") String thirdUserId,
                                        @RequestParam(required = false, defaultValue = "0") Long userId,
                                        @RequestParam(required = false, defaultValue = "PC") String platform,
                                        @RequestParam(required = false, defaultValue = "1") Integer accountType,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        if ((userId == null || userId == 0) && Strings.isNullOrEmpty(thirdUserId)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER);
        }
        try {
            if (platform.equalsIgnoreCase(RequestPlatform.MOBILE.name())) {
                header = header.toBuilder().appBaseHeader(HeaderUtil.buildAppRequestHeaderFromRequest(request)).platform(RequestPlatform.MOBILE).build();
            }
            ThirdPartyUserTokenResult result = accountService.createThirdPartyToken(header, thirdUserId, userId, platform, accountType);
            return JsonUtil.defaultGson().toJson(result);
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case THIRD_TOKEN_EXIST:
                    throw new OpenApiException(ApiErrorCode.THIRD_TOKEN_EXIST);
                case NO_PERMISSION_TO_CREATE_VIRTUAL_ACCOUNT:
                    throw new OpenApiException(ApiErrorCode.NO_PERMISSION_TO_CREATE_VIRTUAL_ACCOUNT);
                case THIRD_USER_EXIST:
                    throw new OpenApiException(ApiErrorCode.THIRD_USER_EXIST);
                case THIRD_USER_NOT_EXIST:
                    throw new OpenApiException(ApiErrorCode.THIRD_USER_NOT_EXIST);
                case BIND_THIRD_USER_ERROR:
                    throw new OpenApiException(ApiErrorCode.BIND_THIRD_USER_ERROR);
                default:
                    log.error("create third_party user response error:{}", errorCode.toString());
                    throw new OpenApiException(ApiErrorCode.GET_THIRD_TOKEN_ERROR);
            }
        } catch (OpenApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Create third party token error e {} orgId {} userId {}", e, header.getOrgId(), userId);
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/logout"})
    public String clearToken(Header header,
                             HttpServletRequest request,
                             @RequestParam(required = false, defaultValue = "") String thirdUserId,
                             @RequestParam(required = false, defaultValue = "0") Long userId,
                             @RequestParam(required = false, defaultValue = "PC") String platform) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        if ((userId == null || userId == 0) && Strings.isNullOrEmpty(thirdUserId)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER);
        }
        accountService.clearToken(header, thirdUserId, userId, platform);
        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("success", Boolean.TRUE);
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON}, forceCheckIpWhiteList = true)
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/transfer_in"})
    public String transferIn(Header header,
                             HttpServletRequest request,
                             @RequestParam String clientOrderId,
                             @RequestParam(required = false, defaultValue = "") String thirdUserId,
                             @RequestParam(required = false, defaultValue = "0") Long userId,
                             @RequestParam Integer accountType,
                             @RequestParam String tokenId,
                             @RequestParam String amount) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        if ((userId == null || userId == 0) && Strings.isNullOrEmpty(thirdUserId)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER);
        }
        accountService.transferIn(header, thirdUserId, userId, AccountType.toAccountTypeEnum(accountType), clientOrderId, tokenId, amount);
        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("success", Boolean.TRUE);
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON}, forceCheckIpWhiteList = true)
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/transfer_out"})
    public String transferOut(Header header,
                              HttpServletRequest request,
                              @RequestParam String clientOrderId,
                              @RequestParam(required = false, defaultValue = "") String thirdUserId,
                              @RequestParam(required = false, defaultValue = "0") Long userId,
                              @RequestParam Integer accountType,
                              @RequestParam String tokenId,
                              @ValidHbtcParam(message = "30003", type = HbtcParamType.HBTC_TRANSFER_AMOUNT)
                              @RequestParam String amount) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        if ((userId == null || userId == 0) && Strings.isNullOrEmpty(thirdUserId)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER);
        }
        accountService.transferOut(header, thirdUserId, userId, AccountType.toAccountTypeEnum(accountType), clientOrderId, tokenId, amount);
        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("success", Boolean.TRUE);
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/balance"})
    public String getBalance(Header header,
                             @RequestParam(required = false, defaultValue = "") String thirdUserId,
                             @RequestParam(required = false, defaultValue = "0") Long userId,
                             @RequestParam Integer accountType) {
        if ((userId == null || userId == 0) && Strings.isNullOrEmpty(thirdUserId)) {
            throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER);
        }
        List<BalanceResult> balanceResults = accountService.getThirdUserBalance(header, thirdUserId, userId, AccountType.toAccountTypeEnum(accountType));
        return JsonUtil.defaultGson().toJson(balanceResults);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON}, forceCheckIpWhiteList = true)
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @PostMapping("/api_key/create")
    public String createApiKey(Header header,
                               HttpServletRequest request,
                               @RequestParam(required = false, defaultValue = "") String thirdUserId,
                               @RequestParam(required = false, defaultValue = "0") Long userId,
                               @RequestParam String tag,
                               @RequestParam(required = false, defaultValue = "1") Integer type) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        ApiKeyResult apiKeyResult = userSecurityService.createApiKey(header, thirdUserId, userId, tag, type);
        return JsonUtil.defaultGson().toJson(apiKeyResult);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON}, forceCheckIpWhiteList = true)
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @PostMapping("/api_key/update_ips")
    public String updateIpWhiteList(Header header,
                                    HttpServletRequest request,
                                    @RequestParam(required = false, defaultValue = "") String thirdUserId,
                                    @RequestParam(required = false, defaultValue = "0") Long userId,
                                    @RequestParam(name = "id") Long apiKeyId,
                                    @RequestParam(name = "ipWhiteList", required = false, defaultValue = "") String ipWhiteList) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
//        ApiKeyResult apiKeyResult =
        userSecurityService.updateApiKeyIps(header, thirdUserId, userId, apiKeyId, ipWhiteList);
//        return JsonUtil.defaultGson().toJson(apiKeyResult);
        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("success", Boolean.TRUE);
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON}, forceCheckIpWhiteList = true)
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @PostMapping("/api_key/change_status")
    public String updateApiKey(Header header,
                               HttpServletRequest request,
                               @RequestParam(required = false, defaultValue = "") String thirdUserId,
                               @RequestParam(required = false, defaultValue = "0") Long userId,
                               @RequestParam(name = "id") Long apiKeyId,
                               @RequestParam Integer status) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
//        ApiKeyResult apiKeyResult =
        userSecurityService.updateApiKeyStatus(header, thirdUserId, userId, apiKeyId, status);
//        return JsonUtil.defaultGson().toJson(apiKeyResult);
        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("success", Boolean.TRUE);
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON}, forceCheckIpWhiteList = true)
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @PostMapping("/api_key/delete")
    public String deleteApiKey(Header header,
                               HttpServletRequest request,
                               @RequestParam(required = false, defaultValue = "") String thirdUserId,
                               @RequestParam(required = false, defaultValue = "0") Long userId,
                               @RequestParam(name = "id") Long apiKeyId) {
        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        userSecurityService.deleteApiKey(header, thirdUserId, userId, apiKeyId);
        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("success", Boolean.TRUE);
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping("/api_keys")
    public String queryUserApiKeys(Header header,
                                   @RequestParam(required = false, defaultValue = "") String thirdUserId,
                                   @RequestParam(required = false, defaultValue = "0") Long userId) {
        List<ApiKeyResult> apiKeyResultList = userSecurityService.queryApiKeys(header, thirdUserId, userId);
        return JsonUtil.defaultGson().toJson(apiKeyResultList);
    }

}
