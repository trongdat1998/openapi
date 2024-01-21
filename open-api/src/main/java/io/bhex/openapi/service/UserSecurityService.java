package io.bhex.openapi.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.security.ApiKeyStatus;
import io.bhex.broker.grpc.user.*;
import io.bhex.openapi.domain.ApiKeyResult;
import io.bhex.openapi.grpc.client.GrpcUserSecurityService;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserSecurityService {

    @Resource
    private GrpcUserSecurityService grpcUserSecurityService;

    /**
     * Create ApiKey
     */
    public ApiKeyResult createApiKey(Header header, String thirdUserId, Long userId, String tag, Integer type) {
        if (Strings.isNullOrEmpty(tag)) {
            throw new BrokerException(BrokerErrorCode.API_KEY_TAG_CANNOT_BE_NULL);
        }
        CreateThirdPartyUserApiKeyRequest request = CreateThirdPartyUserApiKeyRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .setAccountType(AccountTypeEnum.COIN)
                .setAccountIndex(0)
                .setTag(tag)
                .setType(type)
                .build();
        CreateApiKeyResponse response = grpcUserSecurityService.createThirdPartyUserApiKey(request);
        return getApiKeyResult(response.getApiKey());
    }

    public void updateApiKeyIps(Header header, String thirdUserId, Long userId,
                                Long apiKeyId, String ipWhiteList) {
        UpdateThirdPartyUserApiKeyIpsRequest request = UpdateThirdPartyUserApiKeyIpsRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .setApiKeyId(apiKeyId)
                .setIpWhiteList(Strings.nullToEmpty(ipWhiteList))
                .build();
        UpdateApiKeyResponse response = grpcUserSecurityService.updateThirdPartyUserApiKeyIps(request);
    }

    public void updateApiKeyStatus(Header header, String thirdUserId, Long userId,
                                   Long apiKeyId, Integer status) {

        if (status != ApiKeyStatus.ENABLE_VALUE && status != ApiKeyStatus.NOT_ENABLE_VALUE) {
            throw new BrokerException(BrokerErrorCode.PARAM_INVALID);
        }
        UpdateThirdPartyUserApiKeyStatusRequest request = UpdateThirdPartyUserApiKeyStatusRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .setApiKeyId(apiKeyId)
                .setStatus(status)
                .build();
        UpdateApiKeyResponse response = grpcUserSecurityService.updateThirdPartyUserApiKeyStatus(request);
    }

    /**
     * Delete user's ApiKey
     */
    public void deleteApiKey(Header header, String thirdUserId, Long userId, Long apiKeyId) {
        DeleteThirdPartyUserApiKeyRequest request = DeleteThirdPartyUserApiKeyRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .setApiKeyId(apiKeyId)
                .build();
        DeleteApiKeyResponse response = grpcUserSecurityService.deleteThirdPartyUserApiKey(request);
    }

    /**
     * Query user's ApiKey records
     */
    public List<ApiKeyResult> queryApiKeys(Header header, String thirdUserId, Long userId) {
        QueryThirdPartyUserApiKeyRequest request = QueryThirdPartyUserApiKeyRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .build();
        QueryApiKeyResponse response = grpcUserSecurityService.queryThirdPartyUserApiKeys(request);
        if (response.getApiKeysList() != null) {
            return response.getApiKeysList().stream().map(this::getApiKeyResult).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    private ApiKeyResult getApiKeyResult(ApiKey apiKey) {
        return ApiKeyResult.builder()
                .id(apiKey.getId())
//                .accountId(apiKey.getAccountId())
//                .accountType(AccountType.fromAccountTypeEnum(apiKey.getAccountType()).value())
//                .accountIndex(apiKey.getIndex())
//                .accountName(apiKey.getAccountName())
                .apiKey(apiKey.getApiKey())
                .securityKey(Strings.nullToEmpty(apiKey.getSecretKey()))
                .tag(apiKey.getTag())
                .type(apiKey.getType())
                .level(apiKey.getLevel())
                .ipWhiteList(apiKey.getIpWhiteList())
                .status(apiKey.getStatus())
                .created(apiKey.getCreated())
                .updated(apiKey.getUpdated())
                .build();
    }

}
