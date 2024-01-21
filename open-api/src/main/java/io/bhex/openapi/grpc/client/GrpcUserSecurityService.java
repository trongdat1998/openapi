package io.bhex.openapi.grpc.client;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.user.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcUserSecurityService extends GrpcBaseService {

    public CreateApiKeyResponse createThirdPartyUserApiKey(CreateThirdPartyUserApiKeyRequest request) {
        UserSecurityServiceGrpc.UserSecurityServiceBlockingStub stub = grpcClientConfig.userSecurityServiceBlockingStub();
        try {
            CreateApiKeyResponse response = stub.createThirdPartyUserApiKey(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public UpdateApiKeyResponse updateThirdPartyUserApiKeyIps(UpdateThirdPartyUserApiKeyIpsRequest request) {
        UserSecurityServiceGrpc.UserSecurityServiceBlockingStub stub = grpcClientConfig.userSecurityServiceBlockingStub();
        try {
            UpdateApiKeyResponse response = stub.updateThirdPartyUserApiKeyIps(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public UpdateApiKeyResponse updateThirdPartyUserApiKeyStatus(UpdateThirdPartyUserApiKeyStatusRequest request) {
        UserSecurityServiceGrpc.UserSecurityServiceBlockingStub stub = grpcClientConfig.userSecurityServiceBlockingStub();
        try {
            UpdateApiKeyResponse response = stub.updateThirdPartyUserApiKeyStatus(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public DeleteApiKeyResponse deleteThirdPartyUserApiKey(DeleteThirdPartyUserApiKeyRequest request) {
        UserSecurityServiceGrpc.UserSecurityServiceBlockingStub stub = grpcClientConfig.userSecurityServiceBlockingStub();
        try {
            DeleteApiKeyResponse response = stub.deleteThirdPartyUserApiKey(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public QueryApiKeyResponse queryThirdPartyUserApiKeys(QueryThirdPartyUserApiKeyRequest request) {
        UserSecurityServiceGrpc.UserSecurityServiceBlockingStub stub = grpcClientConfig.userSecurityServiceBlockingStub();
        try {
            QueryApiKeyResponse response = stub.queryThirdPartyUserApiKeys(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

}
