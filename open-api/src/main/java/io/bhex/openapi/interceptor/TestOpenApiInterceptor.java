package io.bhex.openapi.interceptor;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.bhex.base.log.LogBizConstants;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.core.domain.BrokerCoreConstants;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.security.SecurityValidApiAccessRequest;
import io.bhex.broker.grpc.security.SecurityValidApiAccessResponse;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.AccountType;
import io.bhex.openapi.domain.BrokerConstants;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.dto.RateLimit;
import io.bhex.openapi.interceptor.annotation.FinanceSupportAuth;
import io.bhex.openapi.interceptor.annotation.LimitAuth;
import io.bhex.openapi.interceptor.annotation.SignAuth;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.service.RateLimitService;
import io.bhex.openapi.service.SecurityService;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class TestOpenApiInterceptor implements HandlerInterceptor {

    private static final String CHECK_API_KEY_WHITE = "_checkApiKeyWhite";
    private static final String API_KEY_WHITE_LIST = "_API_KEY_WHITE_CONFIG";

    private static final String ACCESS_KEY_HEADER = "X-BH-APIKEY";
    private static final String ACCESS_KEY_NAME = "accessKey";
    private static final String SIGNATURE_NAME = "signature";
    private static final String TIMESTAMP_NAME = "timestamp";

    private static final String RECV_WINDOW_NAME = "recvWindow";

    public static final String API_ACCOUNT_ATTR = OpenApiInterceptor.class + ".API_HEADER_ACCOUNT";
    public static final String API_ACCOUNT_TYPE_ATTR = OpenApiInterceptor.class + ".API_HEADER_ACCOUNT_TYPE";
    public static final String API_ACCOUNT_INDEX_ATTR = OpenApiInterceptor.class + ".API_HEADER_ACCOUNT_INDEX";
    public static final String API_SPECIAL_PERMISSION_ATTR = OpenApiInterceptor.class + ".API_HEADER_SPECIAL_PERMISSION";

    @Resource
    private SecurityService securityService;

    @Resource
    private RateLimitService rateLimitService;

    @Resource
    private BasicService basicService;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        Header header = (Header) request.getAttribute(BrokerCoreConstants.HEADER_REQUEST_ATTR);
        if (header == null) {
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 是否需要签名
        SignAuth signAuth = handlerMethod.getMethodAnnotation(SignAuth.class);
        if (signAuth == null) {
            return true;
        }

        // 获取accessKey
        String accessKey = request.getHeader(ACCESS_KEY_HEADER);
        if (Strings.isNullOrEmpty(accessKey)) {
            accessKey = request.getParameter(ACCESS_KEY_NAME);
        }
        if (StringUtils.isEmpty(accessKey) || accessKey.length() < 64) {
            throw new OpenApiException(ApiErrorCode.UNAUTHORIZED);
        }

//        RateLimitType[] typeArray = limitAuth.limitTypes();
//        if (typeArray.length > 0) {
//            for (RateLimitType limitType : typeArray) {
//                for (RateLimitInterval limitInterval : RateLimitInterval.values()) {
//                    String limitKey = rateLimitService.getReachedRateLimitCacheKey(limitType, limitInterval, accessKey);
//                    if (redisTemplate.hasKey(limitKey)) {
//                        if (limitType == RateLimitType.ORDERS) {
//                            throw new OpenApiException(ApiErrorCode.TOO_MANY_ORDERS, redisTemplate.opsForValue().get(limitKey), limitInterval.toString());
//                        } else if (limitType == RateLimitType.REQUEST_WEIGHT) {
//                            throw new OpenApiException(ApiErrorCode.TOO_MANY_REQUESTS, redisTemplate.opsForValue().get(limitKey), limitInterval.toString());
//                        }
//                    }
//                }
//            }
//        }

        List<String> paramNames = Collections.list(request.getParameterNames());

        // 签名时必须得有参数， 要是没有参数， 那就不对了
        if (paramNames.size() <= 0) {
            throw new OpenApiException(ApiErrorCode.UNKNOWN_PARAM);
        }

        AccountType accountType = AccountType.COMMON;
        Long accountId = Long.parseLong(request.getHeader("account_id"));
        Long userId = Long.parseLong(request.getHeader("user_id"));
        AccountType[] requiredAccountTypes = signAuth.requiredAccountTypes();
        if (!checkAccountType(requiredAccountTypes, accountType)) {
            throw new OpenApiException(ApiErrorCode.NO_PERMISSION);
        }
        header = header.toBuilder().userId(userId).build();
        // TODO checkApiLevel
        request.setAttribute(BrokerCoreConstants.HEADER_REQUEST_ATTR, header);
        request.setAttribute(BrokerConstants.API_KEY_TYPE, 1);
        MDC.put(LogBizConstants.ACCOUNT_ID, "" + accountId);
        request.setAttribute(API_ACCOUNT_ATTR, accountId);
        request.setAttribute(API_ACCOUNT_TYPE_ATTR, AccountTypeEnum.COIN);
        request.setAttribute(API_ACCOUNT_INDEX_ATTR, 0);
        request.setAttribute(API_SPECIAL_PERMISSION_ATTR, 0);

        // 校验财务专用API，只有白名单的用户才能访问
//        FinanceSupportAuth financeSupportAuth = handlerMethod.getMethodAnnotation(FinanceSupportAuth.class);
//        if (financeSupportAuth != null) {
//            if (basicService.isUserInCoinWhiteList(validResponse.getUserId())) {
//                return true;
//            } else {
//                throw new OpenApiException(ApiErrorCode.NO_PERMISSION);
//            }
//        }

        return true;
    }

    private boolean checkAccountType(AccountType[] requiredAccountTypes, AccountType apiKeyAccountType) {
        for (AccountType requiredAccountType : requiredAccountTypes) {
            if (requiredAccountType == apiKeyAccountType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(LogBizConstants.ACCOUNT_ID);
    }

    public String buildQueryString(String queryString, String paramName) {
        String[] paramArray = queryString.split("&");
        List<String> paramList = Lists.newArrayList();
        for (String param : paramArray) {
            if (param.indexOf(paramName) > -1) {
                continue;
            }
            paramList.add(param);
        }

        return paramList.stream().collect(Collectors.joining("&"));
    }

    public String buildRequestBodyString(HttpServletRequest request) {

        String queryString = request.getQueryString();
        Map<String, String> queryMap = Maps.newHashMap();

        if (!StringUtils.isEmpty(queryString)) {
            for (String queryValue : queryString.split("&")) {
                if (StringUtils.isEmpty(queryValue)) {
                    continue;
                }
                String[] keyValue = queryValue.split("=");
                queryMap.put(keyValue[0], "1");
            }
        }
        List<String> paramNameList = Collections.list(request.getParameterNames());
        if (CollectionUtils.isEmpty(paramNameList)) {
            return null;
        }

        List<String> paramList = Lists.newArrayList();
        for (String paramName : paramNameList) {
            if (queryMap.get(paramName) != null) {
                continue;
            }
            if (SIGNATURE_NAME.equals(paramName)) {
                continue;
            }
            paramList.add(paramName + "=" + request.getParameter(paramName));
        }

        if (CollectionUtils.isEmpty(paramList)) {
            return null;
        }

        return paramList.stream().collect(Collectors.joining("&"));
    }

}
