package io.bhex.openapi.grpc.client;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.margin.*;
import io.bhex.broker.grpc.otc.third.party.MoonpayTransactionRequest;
import io.bhex.broker.grpc.otc.third.party.MoonpayTransactionResponse;
import io.bhex.broker.grpc.otc.third.party.OtcThirdPartyServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author cookie.yuan
 * @description
 * @date 2020-12-23 10:30
 */
@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcOtcThirdPartyService extends GrpcBaseService {

    public MoonpayTransactionResponse moonpayTransaction(MoonpayTransactionRequest request) {
        OtcThirdPartyServiceGrpc.OtcThirdPartyServiceBlockingStub stub = grpcClientConfig.otcThirdPartyServiceBlockingStub();
        try {
            MoonpayTransactionResponse response = stub.moonpayTransaction(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("moonpayTransaction {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

}
