package io.bhex.openapi.controller;

import com.alibaba.fastjson.JSONObject;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.openapi.domain.AccountType;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.interceptor.OpenApiInterceptor;
import io.bhex.openapi.interceptor.annotation.LimitAuth;
import io.bhex.openapi.interceptor.annotation.SignAuth;
import io.bhex.openapi.service.UserDataStreamService;
import io.bhex.openapi.util.ReadOnlyApiKeyCheckUtil;
import io.bhex.openapi.util.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping(value = {"/openapi", "/openapi/account"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class UserDataApiController {

    @Autowired
    UserDataStreamService userDataStreamService;

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.OPTION, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @PostMapping(value = {"userDataStream", "/v1/userDataStream"})
    public String startUserDataStream(HttpServletRequest request,
                                      Header header,
                                      @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                      @RequestParam(name = "timestamp", required = false) Long timestamp) {
//        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        AccountTypeEnum accountType = (AccountTypeEnum) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_TYPE_ATTR);
        Integer accountIndex = (Integer) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_INDEX_ATTR);
        String listenKey = userDataStreamService.createListenKey(header, accountId, accountType, accountIndex);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("listenKey", listenKey);
        return jsonObject.toJSONString();
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.OPTION, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @PutMapping(value = {"userDataStream", "/v1/userDataStream"})
    public String updateUserDataStream(HttpServletRequest request,
                                       Header header,
                                       @RequestParam(name = "listenKey", required = true) String listenKey,
                                       @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                       @RequestParam(name = "timestamp", required = false) Long timestamp) {
//        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
        userDataStreamService.updateListenKey(listenKey);

        return ResultUtils.toRestJSONString(new Object());
    }

    @SignAuth(requiredAccountTypes = {AccountType.COMMON, AccountType.MAIN, AccountType.OPTION, AccountType.FUTURES})
    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @DeleteMapping(value = {"userDataStream", "/v1/userDataStream"})
    public String closeUserDataStream(HttpServletRequest request,
                                      Header header,
                                      @RequestParam(name = "listenKey", required = true) String listenKey,
                                      @RequestParam(name = "recvWindow", required = false) Long recvWindow,
                                      @RequestParam(name = "timestamp", required = false) Long timestamp) {
//        ReadOnlyApiKeyCheckUtil.checkApiKeyReadOnly(request);
//        Long accountId = (Long) request.getAttribute(OpenApiInterceptor.API_ACCOUNT_ATTR);
        userDataStreamService.deleteListenKey(header, listenKey);

        return ResultUtils.toRestJSONString(new Object());
    }

}
