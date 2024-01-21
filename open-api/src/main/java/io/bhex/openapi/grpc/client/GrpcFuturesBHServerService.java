package io.bhex.openapi.grpc.client;

import io.bhex.base.account.GetDepthInfoRequest;
import io.bhex.base.account.GetDepthInfoResponse;
import io.bhex.base.account.GetTotalPositionReply;
import io.bhex.base.account.GetTotalPositionRequest;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcFuturesBHServerService extends GrpcBaseService {

    public GetDepthInfoResponse getDepthInfo(GetDepthInfoRequest request, Long orgId) {
        try {
            return grpcClientConfig.futuresServerBlockingStub(orgId).getDepthInfo(request);
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetTotalPositionReply getTotalPosition(GetTotalPositionRequest request, Long orgId) {
        try {
            return grpcClientConfig.futuresServerBlockingStub(orgId).getTotalPosition(request);
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
}
