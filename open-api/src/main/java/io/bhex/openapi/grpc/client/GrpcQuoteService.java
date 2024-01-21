package io.bhex.openapi.grpc.client;

import com.google.common.cache.Cache;
import com.google.protobuf.TextFormat;
import io.bhex.base.quote.*;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcQuoteService extends GrpcBasicService{

    public GetRealtimeReply getRealtime(GetQuoteRequest request, Long orgId) {
        QuoteServiceGrpc.QuoteServiceBlockingStub stub = grpcClientConfig.quoteServiceBlockingStub(orgId);
        try {
            return stub.getRealtime(request);
        } catch (StatusRuntimeException e) {
            log.error(" getRealtime error: "+ TextFormat.shortDebugString(request), e);
            throw commonStatusRuntimeException(e);
        }
    }

    public GetDepthReply getPartialDepth(GetQuoteRequest request, Long orgId) {
        try {
            return grpcClientConfig.quoteServiceBlockingStub(orgId).getPartialDepth(request);
        } catch (StatusRuntimeException e) {
            log.error("getPartialDepth error:"+TextFormat.shortDebugString(request), e);
            throw commonStatusRuntimeException(e);
        }
    }

    public GetIndicesReply getIndices(GetIndicesRequest request,  Long orgId) {
        try {
            return grpcClientConfig.quoteServiceBlockingStub(orgId).getIndices(request);
        } catch (StatusRuntimeException e) {
            log.error("getIndices error:"+TextFormat.shortDebugString(request), e);
            throw commonStatusRuntimeException(e);
        }
    }



}
