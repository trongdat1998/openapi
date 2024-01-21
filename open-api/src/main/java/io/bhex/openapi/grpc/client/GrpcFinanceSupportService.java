package io.bhex.openapi.grpc.client;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.finance_support.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcFinanceSupportService extends GrpcBaseService {

    public CreateFinanceAccountResponse createFinanceAccount(CreateFinanceAccountRequest request) {
        try {
            CreateFinanceAccountResponse response = stub().createFinanceAccount(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("createFinanceAccount error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public QueryFinanceAccountsResponse queryFinanceAccounts(QueryFinanceAccountsRequest request) {
        try {
            QueryFinanceAccountsResponse response = stub().queryFinanceAccounts(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("queryFinanceAccounts error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public FinanceAccountTransferResponse financeAccountTransfer(FinanceAccountTransferRequest request) {
        try {
            FinanceAccountTransferResponse response = stub().financeAccountTransfer(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("financeAccountTransfer error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    private FinanceSupportServiceGrpc.FinanceSupportServiceBlockingStub stub() {
        return grpcClientConfig.financeSupportServiceBlockingStub();
    }
}
