package io.bhex.openapi.service;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.grpc.common.Header;
import io.bhex.broker.grpc.security.SecurityValidApiAccessRequest;
import io.bhex.broker.grpc.security.SecurityValidApiAccessResponse;
import io.bhex.broker.grpc.security.SecurityValidApiKeyRequest;
import io.bhex.broker.grpc.security.SecurityValidApiKeyResponse;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.grpc.client.GrpcSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SecurityService {

    @Autowired
    GrpcSecurityService grpcSecurityService;

    public SecurityValidApiAccessResponse validApiKeySign(Header header, SecurityValidApiAccessRequest request) {
        try {
            SecurityValidApiAccessResponse response = grpcSecurityService.validApiKey(header, request);
            return response;
        } catch (BrokerException brokerException) {

            // 业务返回码和OPEN API返回码的转换
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case API_KEY_NOT_ENABLE:
                    throw new OpenApiException(ApiErrorCode.REJECTED_MBX_KEY);
                case IP_NOT_IN_WHITELIST:
                    throw new OpenApiException(ApiErrorCode.REJECTED_MBX_KEY);
                case REQUEST_INVALID:
                    throw new OpenApiException(ApiErrorCode.INVALID_SIGNATURE);
                case BIND_IP_WHITE_LIST_FIRST:
                    throw new OpenApiException(ApiErrorCode.BIND_IP_WHITE_LIST_FIRST);
                default:
                    throw new OpenApiException(ApiErrorCode.UNKNOWN);

            }
        }
    }

    public SecurityValidApiKeyResponse validApiKey(Header header, String apiKey) {
        try {
            SecurityValidApiKeyResponse response = grpcSecurityService.validApiKey(header, SecurityValidApiKeyRequest.newBuilder().setApiKey(apiKey).build());
            return response;
        } catch (BrokerException brokerException) {
            BrokerErrorCode errorCode = BrokerErrorCode.fromCode(brokerException.getCode());
            switch (errorCode) {
                case REQUEST_INVALID:
                    log.info(" validApiKey failed: apiKey:{} --> brokerErrorCode:{}", apiKey, errorCode.code());
                    throw new OpenApiException(ApiErrorCode.REJECTED_MBX_KEY);
                case API_KEY_NOT_ENABLE:
                    throw new OpenApiException(ApiErrorCode.REJECTED_MBX_KEY);
                default:
                    throw new OpenApiException(ApiErrorCode.UNKNOWN);

            }
        }
    }

}
