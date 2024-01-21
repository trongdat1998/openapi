package io.bhex.openapi.service;

import io.bhex.base.margin.Margin;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.grpc.margin.*;
import io.bhex.broker.grpc.user.SaveUserContractRequest;
import io.bhex.broker.grpc.user.SaveUserContractResponse;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.api.result.*;
import io.bhex.openapi.grpc.client.GrpcMarginService;
import io.bhex.openapi.grpc.client.GrpcUserService;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-05 10:12
 */
@Service
@Slf4j
public class MarginService {
    public final static Integer ON_OPEN_TYPE = 1;

    @Resource
    GrpcUserService grpcUserService;
    @Resource
    GrpcMarginService grpcMarginService;
    @Resource
    BasicService basicService;

    public boolean saveUserMarginContract(Header header, String contractName) {
        SaveUserContractRequest request = SaveUserContractRequest.newBuilder()
                .setOrgId(header.getOrgId())
                .setUserId(header.getUserId())
                .setOpen(ON_OPEN_TYPE)
                .setName(contractName)
                .build();
        grpcUserService.saveUserContract(request);
        log.info("{}-{} openUserContract:{}", header.getOrgId(), header.getUserId(), contractName);
        return true;
    }

    public GetMarginSafetyResponse getMarginSafety(Header header) {
        GetMarginSafetyRequest request = GetMarginSafetyRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(0L)
                .build();
        GetMarginSafetyResponse response = grpcMarginService.getMarginSafety(request);
        return response;
    }

    public List<MarginTokenResult> getMarginToken(Header header) {
        List<InterestConfig> interestConfigs = basicService.getMarginInterestConfig(header.getOrgId());
        Map<String, String> interestMap = interestConfigs.stream().collect(Collectors.toMap(InterestConfig::getTokenId, InterestConfig::getInterest, (p, q) -> p));
        List<TokenConfig> tokenConfigList = basicService.getMarginTokenConfig(header.getOrgId());
        return tokenConfigList.stream()
                .map(token -> {
                    String interest = interestMap.getOrDefault(token.getTokenId(), "0");
                    MarginTokenResult result = MarginTokenResult.builder()
                            .exchangeId(token.getExchangeId())
                            .tokenId(token.getTokenId())
                            .convertRate(token.getConvertRate())
                            .leverage(token.getLeverage())
                            .canBorrow(token.getCanBorrow())
                            .maxQuantity(token.getMaxQuantity())
                            .minQuantity(token.getMinQuantity())
                            .quantityPrecision(String.valueOf(token.getQuantityPrecision()))
                            .repayMinQuantity(token.getRepayMinQuantity())
                            .interest(interest)
                            .isOpen(token.getIsOpen())
                            .build();
                    return result;
                }).collect(Collectors.toList());
    }

    public MarginRiskResult getMarginRisk(Header header) {
        RiskConfig risk = basicService.getMarginRisk(header.getOrgId());
        if (risk == null) {
            throw new OpenApiException(ApiErrorCode.RISK_IS_NOT_EXIT);
        }
        return MarginRiskResult.builder()
                .withdrawLine(risk.getWithdrawLine())
                .warnLine(risk.getWarnLine())
                .appendLine(risk.getAppendLine())
                .stopLine(risk.getStopLine())
                .build();
    }

    public GetLoanableResponse getLoanable(Header header,String tokenId){
        GetLoanableRequest request = GetLoanableRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .build();
        return grpcMarginService.getLoanable(request);
    }

    public List<MarginLoanPositionResult> getLoanPosition(Header header,String tokenId){
        GetAllCrossLoanPositionRequest request = GetAllCrossLoanPositionRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .build();
        GetAllCrossLoanPositionResponse resp = grpcMarginService.getAllCrossLoanPosition(request);
        return resp.getCrossLoanPositionList().stream()
                .map(loan -> MarginLoanPositionResult.builder()
                .tokenId(loan.getTokenId())
                .loanTotal(loan.getLoanTotal())
                .loanBtcValue(loan.getLoanBtcValue())
                .interestPaid(loan.getInterestPaid())
                .interestUnpaid(loan.getInterestUnpaid())
                .unpaidBtcValue(loan.getUnpaidBtcValue())
                .build())
                .collect(Collectors.toList());
    }
    public GetAvailWithdrawAmountResponse getAvailWithdrawAmount(Header header,String tokenId){
        GetAvailWithdrawAmountRequest request = GetAvailWithdrawAmountRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .build();
        return grpcMarginService.getAvailWithdrawAmount(request);
    }

    public LoanResponse loan(Header header , String clientOrderId,String loanAmount,String tokenId){
        LoanRequest request = LoanRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setClientOrderId(clientOrderId)
                .setLoanAmount(loanAmount)
                .setTokenId(tokenId)
                .build();
        return grpcMarginService.loan(request);

    }
    public RepayByLoanIdResponse repay(Header header,String clientOrderId,Long loanOrderId,Integer repayType,String repayAmount){
        RepayByLoanIdRequest request = RepayByLoanIdRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setClientOrderId(clientOrderId)
                .setLoanOrderId(loanOrderId)
                .setRepayType(MarginRepayTypeEnum.forNumber(repayType))
                .setRepayAmount(repayAmount)
                .build();
        return grpcMarginService.repayByLoanId(request);
    }

    public MarginAllPositionResult getAllPosition(Header header){
        GetAllPositionRequest request = GetAllPositionRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .build();
        GetAllPositionResponse resp = grpcMarginService.getAllPosition(request);
        return MarginAllPositionResult.builder()
                .total(resp.getTotal())
                .loanAmount(resp.getLoanAmount())
                .availMargin(resp.getMarginAmount())
                .occupyMargin(resp.getOccupyMargin())
                .build();

    }
    public List<MarginLoanOrderResult> queryLoanOrders(Header header,String tokenId,Integer status,Long fromLoanId,Long endLoanId,Integer limit){
        GetCrossLoanOrderRequest request = GetCrossLoanOrderRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .setStatus(status)
                .setFromLoanId(fromLoanId)
                .setEndLoanId(endLoanId)
                .setLimit(limit)
                .build();
        GetCrossLoanOrderResponse resp = grpcMarginService.getCrossLoanOrder(request);
        return resp.getCrossLoanOrderList().stream()
                .map(this :: getMarginLoanOrder)
                .collect(Collectors.toList());
    }
    public MarginLoanOrderResult getLoanOrder(Header header,Long loanOrderId){
        GetCrossLoanOrderRequest request = GetCrossLoanOrderRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setLoanId(loanOrderId)
                .setLimit(1)
                .build();
        GetCrossLoanOrderResponse resp = grpcMarginService.getCrossLoanOrder(request);
        if(resp.getCrossLoanOrderList().isEmpty()){
            return null;
        }
        return getMarginLoanOrder(resp.getCrossLoanOrder(0));
    }
    public MarginLoanOrderResult getMarginLoanOrder(CrossLoanOrder order){
        return MarginLoanOrderResult.builder()
                .loanOrderId(order.getLoanOrderId())
                .clientId(order.getClientId())
                .tokenId(order.getTokenId())
                .loanAmount(order.getLoanAmount())
                .repaidAmount(order.getRepaidAmount())
                .unpaidAmount(order.getUnpaidAmount())
                .interestRate(order.getInterestRate1())
                .interestStart(order.getInterestStart())
                .status(order.getStatus())
                .interestPaid(order.getInterestPaid())
                .interestUnpaid(order.getInterestUnpaid())
                .createAt(order.getCreatedAt())
                .updateAt(order.getUpdatedAt())
                .accountId(order.getAccountId())
                .build();
    }
    public List<MarginRepayOrderResult> queryRepayOrders(Header header,String tokenId,Long fromRepayId,Long endRepayId,Integer limit){
        GetRepayRecordRequest request = GetRepayRecordRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .setFromRepayId(fromRepayId)
                .setEndRepayId(endRepayId)
                .setLimit(limit)
                .build();
        GetRepayRecordResponse resp = grpcMarginService.getRepayRecord(request);
        return resp.getRepayRecordList().stream()
                .map(this::getMarginRepayOrder)
                .collect(Collectors.toList());
    }
    public List<MarginRepayOrderResult> getRepayOrder(Header header,Long loanOrderId,Long fromRepayId,Long endRepayId,Integer limit){
        GetRepayRecordRequest request = GetRepayRecordRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setLoanOrderId(loanOrderId)
                .setFromRepayId(fromRepayId)
                .setEndRepayId(endRepayId)
                .setLimit(limit)
                .build();
        GetRepayRecordResponse resp = grpcMarginService.getRepayRecord(request);
        if(resp.getRepayRecordList().isEmpty()){
            return new ArrayList<>();
        }
        return resp.getRepayRecordList().stream()
                .map(this::getMarginRepayOrder)
                .collect(Collectors.toList());
    }

    public MarginRepayOrderResult getMarginRepayOrder(RepayRecord record){
        return MarginRepayOrderResult.builder()
                .repayOrderId(record.getRepayOrderId())
                .accountId(record.getAccountId())
                .clientId(record.getClientId())
                .tokenId(record.getTokenId())
                .loanOrderId(record.getLoanOrderId())
                .amount(record.getAmount())
                .interest(record.getInterest())
                .createAt(record.getCreatedAt())
                .build();
    }

}
