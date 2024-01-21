package io.bhex.openapi.grpc.client;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.transfer.TransferServiceGrpc;
import io.bhex.broker.grpc.transfer.UserTransferToUserRequest;
import io.bhex.broker.grpc.transfer.UserTransferToUserResponse;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcBalanceService extends GrpcBaseService {

    public UserTransferToUserResponse userTransferToUser(UserTransferToUserRequest request) {
        TransferServiceGrpc.TransferServiceBlockingStub stub = grpcClientConfig.transferServiceBlockingStub();
        try {
            UserTransferToUserResponse response = stub.userTransferToUser(request);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
}
