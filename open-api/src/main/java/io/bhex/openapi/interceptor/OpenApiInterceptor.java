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
public class OpenApiInterceptor implements HandlerInterceptor {

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

        StringBuffer paramBuffer = new StringBuffer();

        // 拼接queryString的串
        String queryString = request.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            paramBuffer.append(buildQueryString(queryString, SIGNATURE_NAME));
        }

        // 拼接requestBody的串
        String requestBodyString = buildRequestBodyString(request);
        if (!StringUtils.isEmpty(requestBodyString)) {
            paramBuffer.append(requestBodyString);
        }

        // 签名的传必须得有长度， 否则没得签名
        if (paramBuffer.length() <= 0) {
            throw new OpenApiException(ApiErrorCode.UNKNOWN_PARAM);
        }

        // 获取签名
        String signature = request.getParameter(SIGNATURE_NAME);
        if (Strings.isNullOrEmpty(signature)) {
            throw new MissingServletRequestParameterException(SIGNATURE_NAME, "String");
//            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, SIGNATURE_NAME);
        }

        String timestampParam = request.getParameter(TIMESTAMP_NAME);
        if (Strings.isNullOrEmpty(timestampParam)) {
            throw new MissingServletRequestParameterException(TIMESTAMP_NAME, "Long");
//            throw new OpenApiException(ApiErrorCode.PARAM_EMPTY, TIMESTAMP_NAME);
        }

        if (signAuth.checkRecvWindow()) {
            String clientOrderId = Strings.nullToEmpty(request.getParameter("clientOrderId"));
            String recvWindowString = request.getParameter(RECV_WINDOW_NAME);
            Long recvWindow = Long.valueOf(StringUtils.isEmpty(recvWindowString) ? "5000" : recvWindowString);
            Long timestamp = Long.valueOf(timestampParam);
            Long serverTime = System.currentTimeMillis();
            if (timestamp > (serverTime + 1000) || (serverTime - timestamp) > recvWindow) {
                log.info("request time out! clientOrderId:{} timestamp {} serverTime:{}", clientOrderId, timestamp, serverTime);
                throw new OpenApiException(ApiErrorCode.INVALID_TIMESTAMP);
            }
        }

        io.bhex.broker.grpc.common.Header validHeader = HeaderConvertUtil.convertHeader(header);
        SecurityValidApiAccessRequest verifyRequest = SecurityValidApiAccessRequest.newBuilder()
                .setApiKey(accessKey)
                .setOriginStr(paramBuffer.toString())
                .setSignature(signature)
                .setHeader(validHeader)
                .setForceCheckIpWhiteList(signAuth.forceCheckIpWhiteList())
                .build();
        SecurityValidApiAccessResponse validResponse = securityService.validApiKeySign(validHeader, verifyRequest);
        header = header.toBuilder().userId(validResponse.getUserId()).build();
        // 签名验证， 此处会有返回异常抛出
        // 5 是 白名单。无限制
        LimitAuth limitAuth = handlerMethod.getMethodAnnotation(LimitAuth.class);
        if (limitAuth != null && validResponse.getLevel() != 6) {
            // 限速检查
            RateLimit rateLimit = rateLimitService.checkRateLimit(header, limitAuth, accessKey, validResponse.getLevel(), limitAuth.weight());
            if (rateLimit != null) {
                if (rateLimit.getRateLimitType().equals(RateLimitType.REQUEST_WEIGHT)) {
                    throw new OpenApiException(ApiErrorCode.TOO_MANY_REQUESTS, rateLimit.getLimit() + "", rateLimit.getInterval().toString());
                } else if (rateLimit.getRateLimitType().equals(RateLimitType.ORDERS)) {
                    throw new OpenApiException(ApiErrorCode.TOO_MANY_ORDERS, rateLimit.getLimit() + "", rateLimit.getIntervalUnit() + " " + rateLimit.getInterval().toString());
                } else {
                    log.info(" {} limit rate touch!!! :{}", accessKey, JSON.toJSONString(rateLimit));
                    throw new OpenApiException(ApiErrorCode.UNKNOWN);
                }
            }
        }
        AccountType accountType;
        Long accountId;
        if (validResponse.getAccountType() == AccountTypeEnum.COIN && validResponse.getAccountIndex() == 0) {
            accountType = AccountType.COMMON;
            accountId = 0L;
        } else {
            accountType = AccountType.fromAccountTypeEnum(validResponse.getAccountType());
            accountId = validResponse.getAccountId();
        }
        AccountType[] requiredAccountTypes = signAuth.requiredAccountTypes();
        if (!checkAccountType(requiredAccountTypes, accountType)) {
            throw new OpenApiException(ApiErrorCode.NO_PERMISSION);
        }
        // TODO checkApiLevel
        request.setAttribute(BrokerCoreConstants.HEADER_REQUEST_ATTR, header);
        request.setAttribute(BrokerConstants.API_KEY_TYPE, validResponse.getType());
        MDC.put(LogBizConstants.ACCOUNT_ID, "" + validResponse.getAccountId());
        request.setAttribute(API_ACCOUNT_ATTR, accountId);
        request.setAttribute(API_ACCOUNT_TYPE_ATTR, validResponse.getAccountType());
        request.setAttribute(API_ACCOUNT_INDEX_ATTR, validResponse.getAccountIndex());
        request.setAttribute(API_SPECIAL_PERMISSION_ATTR, validResponse.getSpecialPermission());

        // 校验财务专用API，只有白名单的用户才能访问
        FinanceSupportAuth financeSupportAuth = handlerMethod.getMethodAnnotation(FinanceSupportAuth.class);
        if (financeSupportAuth != null) {
            if (basicService.isUserInCoinWhiteList(validResponse.getUserId())) {
                return true;
            } else {
                throw new OpenApiException(ApiErrorCode.NO_PERMISSION);
            }
        }

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

        return String.join("&", paramList);
    }

}
