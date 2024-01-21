/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.grpc.client
 *@Date 2018/6/25
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.grpc.client;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.NoGrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.common.Header;
import io.bhex.broker.grpc.deposit.*;
import io.bhex.broker.grpc.order.*;
import io.bhex.broker.grpc.withdraw.QueryWithdrawOrdersRequest;
import io.bhex.broker.grpc.withdraw.QueryWithdrawOrdersResponse;
import io.bhex.broker.grpc.withdraw.WithdrawServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcOrderService extends GrpcBaseService {

    public CreateOrderResponse createOrder(Header header, CreateOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            CreateOrderResponse response = stub.createOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CreateOrderResponse createOrderV20(Header header, CreateOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            CreateOrderResponse response = stub.createOrderV20(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CreateOrderResponse createOrderV21(Header header, CreateOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            CreateOrderResponse response = stub.createOrderV21(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CancelOrderResponse cancelOrder(Header header, CancelOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            CancelOrderResponse response = stub.cancelOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public FastCancelOrderResponse fastCancelOrder(Header header, FastCancelOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            FastCancelOrderResponse response = stub.fastCancelOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CancelOrderResponse cancelOrderV20(Header header, CancelOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            CancelOrderResponse response = stub.cancelOrderV20(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CancelOrderResponse cancelOrderV21(Header header, CancelOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            CancelOrderResponse response = stub.cancelOrderV21(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public BatchCancelOrderResponse batchCancel(Header header, BatchCancelOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            BatchCancelOrderResponse response = stub.batchCancelOrder(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetOrderResponse getOrder(Header header, GetOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            GetOrderResponse response = stub.getOrder(request);
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
    public QueryOrdersResponse queryOrders(Header header, QueryOrdersRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QueryOrdersResponse response = stub.queryOrders(request);
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
    public QueryOrdersResponse queryAnyOrders(Header header, QueryAnyOrdersRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QueryOrdersResponse response = stub.queryAnyOrders(request);
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
    public QueryMatchResponse queryMatchInfo(Header header, QueryMatchRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QueryMatchResponse response = stub.queryMatch(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetOrderMatchResponse getOrderMatchInfo(Header header, GetOrderMatchRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            GetOrderMatchResponse response = stub.getOrderMatch(request);
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
    public QueryDepositOrdersResponse queryDepositOrder(QueryDepositOrdersRequest request) {
        DepositServiceGrpc.DepositServiceBlockingStub stub = grpcClientConfig.depositServiceBlockingStub();
        try {
            QueryDepositOrdersResponse response = stub.queryDepositOrders(request);
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
    public QueryWithdrawOrdersResponse queryWithdrawOrder(QueryWithdrawOrdersRequest request) {
        WithdrawServiceGrpc.WithdrawServiceBlockingStub stub = grpcClientConfig.withdrawServiceBlockingStub();
        try {
            QueryWithdrawOrdersResponse response = stub.queryWithdrawOrders(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @NoGrpcLog()
    public GetBestOrderResponse getBestOrder(GetBestOrderRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            return stub.getBestOrder(request);
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @NoGrpcLog()
    public GetDepthInfoResponse getDepthInfo(GetDepthInfoRequest request) {
        OrderServiceGrpc.OrderServiceBlockingStub stub = grpcClientConfig.orderServiceBlockingStub();
        try {
            return stub.getBestDepth(request);
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetUserInfoByAddressResponse getUserInfoByAddress(GetUserInfoByAddressRequest request) {
        DepositServiceGrpc.DepositServiceBlockingStub stub = grpcClientConfig.depositServiceBlockingStub();
        try {
            GetUserInfoByAddressResponse response = stub.getUserInfoByAddress(request);
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
}
