/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.grpc.client
 *@Date 2018/8/22
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.grpc.client;

import com.google.api.client.util.Lists;
import io.bhex.base.token.TokenTypeEnum;
import io.bhex.broker.grpc.basic.*;
import io.bhex.broker.grpc.common_ini.CommonIniServiceGrpc;
import io.bhex.broker.grpc.common_ini.GetCommonIni2Request;
import io.bhex.broker.grpc.common_ini.GetCommonIni2Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import io.bhex.base.token.TokenCategory;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.NoGrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.grpc.broker.BrokerServiceGrpc;
import io.bhex.broker.grpc.broker.QueryBrokerRequest;
import io.bhex.broker.grpc.broker.QueryBrokerResponse;
import io.bhex.broker.grpc.common.Header;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcBasicService extends GrpcBaseService {

    private List<Integer> coinCategories = Arrays.asList(
            TokenCategory.MAIN_CATEGORY.getNumber(),
            TokenCategory.INNOVATION_CATEGORY.getNumber()
    );

    private List<Integer> optionCategories = Collections.singletonList(TokenCategory.OPTION_CATEGORY.getNumber());

    private List<Integer> futuresCategories = Collections.singletonList(TokenCategory.FUTURE_CATEGORY.getNumber());

    @NoGrpcLog
    public QueryBrokerResponse queryBrokers(Header header, QueryBrokerRequest request) {
        BrokerServiceGrpc.BrokerServiceBlockingStub stub = grpcClientConfig.brokerServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QueryBrokerResponse response = stub.queryBrokers(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @NoGrpcLog
    public QuerySymbolResponse querySymbols(Header header, QuerySymbolRequest request) {
        BasicServiceGrpc.BasicServiceBlockingStub stub = grpcClientConfig.basicServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QuerySymbolResponse response = stub.querySymbols(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public List<Token> queryCoinTokens(io.bhex.broker.grpc.common.Header header) {
        QueryTokenRequest request = QueryTokenRequest.newBuilder().addAllCategory(coinCategories).build();
        QueryTokenResponse response = queryTokens(header, request);
        return Optional.ofNullable(response.getTokensList()).orElse(new ArrayList<>())
                .stream()
                .filter(token -> !token.getTokenType().equals(TokenTypeEnum.BH_CARD.name()))
                .collect(Collectors.toList());
    }

    private QueryTokenResponse queryTokens(Header header, QueryTokenRequest request) {
        BasicServiceGrpc.BasicServiceBlockingStub stub = grpcClientConfig.basicServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QueryTokenResponse response = stub.queryTokens(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
    public Long getServerTime(Header header, GetServerTimeRequest request) {
        BasicServiceGrpc.BasicServiceBlockingStub stub = grpcClientConfig.basicServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            return stub.serverTime(request).getServerTime();
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @NoGrpcLog
    public QueryTokenResponse queryOptionTokens(Header header) {
        QueryTokenRequest request = QueryTokenRequest.newBuilder().addAllCategory(optionCategories).build();
        BasicServiceGrpc.BasicServiceBlockingStub stub = grpcClientConfig.basicServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QueryTokenResponse response = stub.queryTokens(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @NoGrpcLog
    public List<Symbol> queryFuturesSymbols(Header header) {
        QuerySymbolRequest request = QuerySymbolRequest.newBuilder().addAllCategory(futuresCategories).build();
        return querySymbols(header, request).getSymbolList();
    }

    @NoGrpcLog
    public List<Long> getWhiteList(String iniName) {
        CommonIniServiceGrpc.CommonIniServiceBlockingStub stub = grpcClientConfig.commonIniServiceBlockingStub();
        GetCommonIni2Request request = GetCommonIni2Request.newBuilder()
                .setIniName(iniName)
                .setLanguage("")
                .setOrgId(0L)
                .build();

        try {
            GetCommonIni2Response response = stub.getCommonIni2(request);
            String iniValue = response.getInis().getIniValue();
            if (StringUtils.isEmpty(iniValue)) {
                return Lists.newArrayList();
            } else {
                return Arrays.stream(StringUtils.split(iniValue, ","))
                        .map(Long::valueOf).collect(Collectors.toList());
            }
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public List<GetEtfSymbolPriceResponse.EtfPrice> getEtfSymbolPrice(GetEtfSymbolPriceRequest request) {
        BasicServiceGrpc.BasicServiceBlockingStub stub = grpcClientConfig.basicServiceBlockingStub();
        try {
            return stub.getEtfSymbolPrice(request).getEtfPriceList();
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
}
