/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.grpc.client
 *@Date 2018/7/27
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.grpc.client;

import com.google.common.base.Strings;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.common.Header;
import io.bhex.broker.grpc.security.*;
import io.bhex.broker.grpc.user.QueryApiKeyRequest;
import io.bhex.broker.grpc.user.QueryApiKeyResponse;
import io.bhex.broker.grpc.user.UserSecurityServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcSecurityService extends GrpcBaseService {

    public SecurityValidApiAccessResponse validApiKey(Header header, SecurityValidApiAccessRequest request) {
        SecurityServiceGrpc.SecurityServiceBlockingStub stub = grpcClientConfig.securityServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }

            if (Strings.isNullOrEmpty(request.getApiKey())) {
                throw new BrokerException(BrokerErrorCode.PARAM_INVALID);
            }

            if (Strings.isNullOrEmpty(request.getOriginStr())) {
                throw new BrokerException(BrokerErrorCode.PARAM_INVALID);
            }

            if (Strings.isNullOrEmpty(request.getSignature())) {
                throw new BrokerException(BrokerErrorCode.PARAM_INVALID);
            }

            SecurityValidApiAccessResponse response = stub.validApiAccess(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public QueryApiKeyResponse queryApiKeys(Header header, QueryApiKeyRequest request) {
        UserSecurityServiceGrpc.UserSecurityServiceBlockingStub stub = grpcClientConfig.userSecurityServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QueryApiKeyResponse response = stub.queryApiKeys(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public SecurityValidApiKeyResponse validApiKey(Header header, SecurityValidApiKeyRequest request) {
        SecurityServiceGrpc.SecurityServiceBlockingStub stub = grpcClientConfig.securityServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            SecurityValidApiKeyResponse response = stub.validApiKey(request);
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
