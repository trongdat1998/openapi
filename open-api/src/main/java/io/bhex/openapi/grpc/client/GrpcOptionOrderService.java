/*
 ************************************
 * @项目名称: api-parent
 * @文件名称: GrpcOptionOrderService
 * @Date 2019/01/09
 * @Author will.zhao@bhex.io
 * @Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 **************************************
 */
package io.bhex.openapi.grpc.client;

import io.bhex.broker.common.grpc.client.annotation.NoGrpcLog;
import org.springframework.stereotype.Service;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.order.BatchCancelOrderRequest;
import io.bhex.broker.grpc.order.BatchCancelOrderResponse;
import io.bhex.broker.grpc.order.CancelOrderRequest;
import io.bhex.broker.grpc.order.CancelOrderResponse;
import io.bhex.broker.grpc.order.CreateOrderRequest;
import io.bhex.broker.grpc.order.CreateOrderResponse;
import io.bhex.broker.grpc.order.GetOrderMatchRequest;
import io.bhex.broker.grpc.order.GetOrderMatchResponse;
import io.bhex.broker.grpc.order.GetOrderRequest;
import io.bhex.broker.grpc.order.GetOrderResponse;
import io.bhex.broker.grpc.order.HistoryOptionRequest;
import io.bhex.broker.grpc.order.HistoryOptionsResponse;
import io.bhex.broker.grpc.order.OptionPositionsRequest;
import io.bhex.broker.grpc.order.OptionPositionsResponse;
import io.bhex.broker.grpc.order.OptionSettlementRequest;
import io.bhex.broker.grpc.order.OptionSettlementResponse;
import io.bhex.broker.grpc.order.OrderServiceGrpc;
import io.bhex.broker.grpc.order.QueryMatchRequest;
import io.bhex.broker.grpc.order.QueryMatchResponse;
import io.bhex.broker.grpc.order.QueryOrdersRequest;
import io.bhex.broker.grpc.order.QueryOrdersResponse;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcOptionOrderService extends GrpcBaseService {

    public CreateOrderResponse createOptionOrder(CreateOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            CreateOrderResponse response = stub.createOptionOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CancelOrderResponse cancelOptionOrder(CancelOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            CancelOrderResponse response = stub.cancelOptionOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public BatchCancelOrderResponse batchCancelOptionOrder(BatchCancelOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            BatchCancelOrderResponse response = stub.batchCancelOptionOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetOrderResponse getOptionOrder(GetOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            GetOrderResponse response = stub.getOptionOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @GrpcLog(printNoResponse = true)
    public QueryOrdersResponse queryOptionOrders(QueryOrdersRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            QueryOrdersResponse response = stub.queryOptionOrders(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @GrpcLog(printNoResponse = true)
    public QueryMatchResponse queryOptionMatchInfo(QueryMatchRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            QueryMatchResponse response = stub.queryOptionMatch(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetOrderMatchResponse getOptionOrderMatchInfo(GetOrderMatchRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            GetOrderMatchResponse response = stub.getOptionOrderMatch(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @GrpcLog(printNoResponse = true)
    public OptionPositionsResponse getOptionPositions(OptionPositionsRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            OptionPositionsResponse response = stub.getOptionPositions(request);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public OptionSettlementResponse getOptionSettlement(OptionSettlementRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            OptionSettlementResponse response = stub.getOptionSettlement(request);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public HistoryOptionsResponse getHistoryOptions(HistoryOptionRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            HistoryOptionsResponse response = stub.getHistoryOptions(request);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
}
