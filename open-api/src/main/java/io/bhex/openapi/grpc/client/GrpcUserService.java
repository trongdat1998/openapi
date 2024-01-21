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
public class GrpcUserService extends GrpcBaseService {

    public GetUserInfoResponse getUserInfo(GetUserInfoRequest request) {
        UserServiceGrpc.UserServiceBlockingStub stub = grpcClientConfig.userServiceBlockingStub();
        try {
            GetUserInfoResponse response = stub.getUserInfo(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public SaveUserContractResponse saveUserContract(SaveUserContractRequest request) {
        UserServiceGrpc.UserServiceBlockingStub stub = grpcClientConfig.userServiceBlockingStub();
        try {
            SaveUserContractResponse response = stub.saveUserContract(request);
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
