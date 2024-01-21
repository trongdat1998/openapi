package io.bhex.openapi.grpc.client;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.common.Header;
import io.bhex.broker.grpc.margin.*;
import io.bhex.broker.grpc.order.CreateOrderRequest;
import io.bhex.broker.grpc.order.CreateOrderResponse;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-05 10:30
 */
@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcMarginService extends GrpcBaseService {

    public GetMarginSafetyResponse getMarginSafety(GetMarginSafetyRequest request) {
        MarginServiceGrpc.MarginServiceBlockingStub stub = grpcClientConfig.marginServiceBlockingStub();
        try {
            GetMarginSafetyResponse response = stub.getMarginSafety(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public GetTokenConfigResponse getTokenConfig(GetTokenConfigRequest request){
        MarginServiceGrpc.MarginServiceBlockingStub stub = grpcClientConfig.marginServiceBlockingStub();
        try {
            GetTokenConfigResponse response = stub.getTokenConfig(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetRiskConfigResponse getRiskConfigResponse(GetRiskConfigRequest request){
        MarginServiceGrpc.MarginServiceBlockingStub stub = grpcClientConfig.marginServiceBlockingStub();
        try {
            GetRiskConfigResponse response = stub.getRiskConfig(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetInterestConfigResponse getInterestConfig(GetInterestConfigRequest request){
        MarginServiceGrpc.MarginServiceBlockingStub stub = grpcClientConfig.marginServiceBlockingStub();
        try {
            GetInterestConfigResponse response = stub.getInterestConfig(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public GetLoanableResponse getLoanable(GetLoanableRequest request){
        MarginServiceGrpc.MarginServiceBlockingStub stub = grpcClientConfig.marginServiceBlockingStub();
        try {
            GetLoanableResponse response = stub.getLoanable(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetAllCrossLoanPositionResponse getAllCrossLoanPosition(GetAllCrossLoanPositionRequest request){
        MarginPositionServiceGrpc.MarginPositionServiceBlockingStub stub = grpcClientConfig.marginPositionServiceBlockingStub();
        try {
            GetAllCrossLoanPositionResponse response = stub.getAllCrossLoanPosition(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public GetAvailWithdrawAmountResponse getAvailWithdrawAmount(GetAvailWithdrawAmountRequest request){
        MarginPositionServiceGrpc.MarginPositionServiceBlockingStub stub = grpcClientConfig.marginPositionServiceBlockingStub();
        try {
            GetAvailWithdrawAmountResponse response = stub.getAvailWithdrawAmount(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public LoanResponse loan(LoanRequest request){
        MarginPositionServiceGrpc.MarginPositionServiceBlockingStub stub = grpcClientConfig.marginPositionServiceBlockingStub();
        try {
            LoanResponse response = stub.loan(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public RepayByLoanIdResponse repayByLoanId(RepayByLoanIdRequest request){
        MarginPositionServiceGrpc.MarginPositionServiceBlockingStub stub = grpcClientConfig.marginPositionServiceBlockingStub();
        try {
            RepayByLoanIdResponse response = stub.repayByLoanId(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public GetAllPositionResponse getAllPosition(GetAllPositionRequest request){
        MarginPositionServiceGrpc.MarginPositionServiceBlockingStub stub = grpcClientConfig.marginPositionServiceBlockingStub();
        try {
            GetAllPositionResponse response = stub.getAllPosition(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public GetCrossLoanOrderResponse getCrossLoanOrder(GetCrossLoanOrderRequest request){
        MarginServiceGrpc.MarginServiceBlockingStub stub = grpcClientConfig.marginServiceBlockingStub();
        try {
            GetCrossLoanOrderResponse response = stub.getCrossLoanOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public GetRepayRecordResponse getRepayRecord(GetRepayRecordRequest request){
        MarginServiceGrpc.MarginServiceBlockingStub stub = grpcClientConfig.marginServiceBlockingStub();
        try {
            GetRepayRecordResponse response = stub.getRepayRecord(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public CreateOrderResponse marginCreateOrder(Header header, CreateOrderRequest request){
        MarginOrderServiceGrpc.MarginOrderServiceBlockingStub stub = grpcClientConfig.marginOrderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            CreateOrderResponse response = stub.marginRiskCreateOrder(request);
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
