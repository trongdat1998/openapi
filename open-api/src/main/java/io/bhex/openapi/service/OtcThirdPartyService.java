package io.bhex.openapi.service;

import io.bhex.broker.common.entity.Header;
import io.bhex.broker.grpc.otc.third.party.MoonpayTransactionRequest;
import io.bhex.broker.grpc.otc.third.party.MoonpayTransactionResponse;
import io.bhex.openapi.grpc.client.GrpcOtcThirdPartyService;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-05 10:12
 */
@Service
@Slf4j
public class OtcThirdPartyService {

    @Resource
    GrpcOtcThirdPartyService grpcOtcThirdPartyService;

    public MoonpayTransactionResponse moonpayTransaction(Header header, String transactionId, String transactionStatus,
                                                         String orderId, String feeAmount,String extraFeeAmount,
                                                         String networkFeeAmount) {
        MoonpayTransactionRequest request = MoonpayTransactionRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTransactionId(transactionId)
                .setTransactionStatus(transactionStatus)
                .setOrderId(orderId)
                .setFeeAmount(feeAmount)
                .setExtraFeeAmount(extraFeeAmount)
                .setNetworkFeeAmount(networkFeeAmount)
                .build();
        MoonpayTransactionResponse response = grpcOtcThirdPartyService.moonpayTransaction(request);
        return response;
    }

}
