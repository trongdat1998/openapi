package io.bhex.openapi.grpc.client;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.NoGrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.order.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcFuturesOrderService extends GrpcBaseService {

    public CreateFuturesOrderResponse createFuturesOrder(CreateFuturesOrderRequest request) {
        try {
            CreateFuturesOrderResponse response = futuresOrderServiceStub().createFuturesOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("createFuturesOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CreateFuturesOrderResponse createFuturesOrderV20(CreateFuturesOrderRequest request) {
        try {
            CreateFuturesOrderResponse response = futuresOrderServiceStub().createFuturesOrderV20(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("createFuturesOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CreateFuturesOrderResponse createFuturesOrderV21(CreateFuturesOrderRequest request) {
        try {
            CreateFuturesOrderResponse response = futuresOrderServiceStub().createFuturesOrderV21(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("createFuturesOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CancelFuturesOrderResponse cancelFuturesOrder(CancelFuturesOrderRequest request) {
        try {
            CancelFuturesOrderResponse response = futuresOrderServiceStub().cancelFuturesOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("cancelFuturesOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CancelFuturesOrderResponse cancelFuturesOrderV20(CancelFuturesOrderRequest request) {
        try {
            CancelFuturesOrderResponse response = futuresOrderServiceStub().cancelFuturesOrderV20(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("cancelFuturesOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CancelFuturesOrderResponse cancelFuturesOrderV21(CancelFuturesOrderRequest request) {
        try {
            CancelFuturesOrderResponse response = futuresOrderServiceStub().cancelFuturesOrderV21(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("cancelFuturesOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetOrderResponse getFuturesOrder(GetOrderRequest request) {
        try {
            GetOrderResponse response = futuresOrderServiceStub().getFuturesOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("getFuturesOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @GrpcLog(printNoResponse = true)
    public QueryFuturesOrdersResponse queryFuturesOrders(QueryFuturesOrdersRequest request) {
        try {
            QueryFuturesOrdersResponse response = futuresOrderServiceStub().queryFuturesOrders(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("queryFuturesOrders error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetPlanOrderResponse getFuturesPlanOrder(GetPlanOrderRequest request) {
        try {
            GetPlanOrderResponse response = futuresOrderServiceStub().getPlanOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("getFuturesPlanOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @GrpcLog(printNoResponse = true)
    public QueryMatchResponse queryFuturesMatchInfo(QueryMatchRequest request) {
        try {
            QueryMatchResponse response = futuresOrderServiceStub().queryFuturesMatch(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("queryFuturesMatchInfo error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public BatchCancelOrderResponse batchCancelFutureOrder(BatchCancelOrderRequest request) {
        try {
            BatchCancelOrderResponse response = futuresOrderServiceStub().batchCancelFuturesOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("batchCancelFutureOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public FuturesPositionsResponse getFuturesPositions(FuturesPositionsRequest request) {
        try {
            FuturesPositionsResponse response = futuresOrderServiceStub().getFuturesPositions(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("getFuturesPositions error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public FuturesSettlementResponse getFuturesSettlement(FuturesSettlementRequest request) {
        try {
            return futuresOrderServiceStub().getFuturesSettlement(request);
        } catch (StatusRuntimeException e) {
            log.error("getFuturesSettlement error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public AddMarginResponse addMargin(AddMarginRequest request) {
        try {
            AddMarginResponse response = futuresOrderServiceStub().addMargin(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("addMargin error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public ReduceMarginResponse reduceMargin(ReduceMarginRequest request) {
        try {
            ReduceMarginResponse response = futuresOrderServiceStub().reduceMargin(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("reduceMargin error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetFundingRatesResponse getFundingRates(GetFundingRatesRequest request) {
        try {
            GetFundingRatesResponse response = futuresOrderServiceStub().getFundingRates(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("getFundingRates error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetHistoryFundingRatesResponse getHistoryFundingRates(GetHistoryFundingRatesRequest request) {
        try {
            GetHistoryFundingRatesResponse response = futuresOrderServiceStub().getHistoryFundingRates(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetInsuranceFundsResponse getInsuranceFunds(GetInsuranceFundsRequest request) {
        try {
            GetInsuranceFundsResponse response = futuresOrderServiceStub().getInsuranceFunds(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("getInsuranceFunds error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetFuturesCoinAssetResponse getFuturesCoinAsset(GetFuturesCoinAssetRequest request) {
        try {
            GetFuturesCoinAssetResponse response = futuresOrderServiceStub().getFuturesCoinAsset(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("getFuturesCoinAsset error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public SetOrderSettingResponse setOrderSetting(SetOrderSettingRequest request) {
        try {
            SetOrderSettingResponse response = futuresOrderServiceStub().setOrderSetting(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("setOrderSetting error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetOrderSettingResponse getOrderSetting(GetOrderSettingRequest request) {
        try {
            GetOrderSettingResponse response = futuresOrderServiceStub().getOrderSetting(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("getOrderSetting error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetFuturesBestOrderResponse getFuturesBestOrder(GetFuturesBestOrderRequest request) {
        try {
            return futuresOrderServiceStub().getFuturesBestOrder(request);
        } catch (StatusRuntimeException e) {
            log.error("getFuturesBestOrder error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public SetRiskLimitResponse setRiskLimit(SetRiskLimitRequest request) {
        try {
            return futuresOrderServiceStub().setRiskLimit(request);
        } catch (StatusRuntimeException e) {
            log.error("setRiskLimit error: {}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public MarketPullFuturesPositionsResponse marketPullFuturesPositions(MarketPullFuturesPositionsRequest request) {
        try {
            MarketPullFuturesPositionsResponse response = futuresOrderServiceStub().marketPullFuturesPositions(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    private FuturesOrderServiceGrpc.FuturesOrderServiceBlockingStub futuresOrderServiceStub() {
        return grpcClientConfig.futuresOrderServiceBlockingStub();
    }
}
