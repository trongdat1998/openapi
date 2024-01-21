package io.bhex.openapi.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.grpc.account.InstitutionalUserTransferRequest;
import io.bhex.broker.grpc.account.InstitutionalUserTransferResponse;
import io.bhex.broker.grpc.deposit.*;
import io.bhex.broker.grpc.withdraw.*;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.*;
import io.bhex.openapi.grpc.client.GrpcAccountService;
import io.bhex.openapi.grpc.client.GrpcOrderService;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BalanceService {

    @Resource
    private GrpcAccountService grpcAccountService;

    @Resource
    private GrpcOrderService grpcOrderService;

    public TransferResult userTransferToUser(Header header,
                                             Long targetUserId,
                                             String clientOrderId,
                                             String amount,
                                             String tokenId,
                                             Integer businessType,
                                             Integer subBusinessType,
                                             String address,
                                             String addressExt) {
        Long userId = 0L;
        if (targetUserId != null && targetUserId > 0) {
            userId = targetUserId;
        } else if (StringUtils.isNotEmpty(address)) {
            BaseUserInfoResult baseUserInfoResult = getUserInfoByAddress(header, tokenId, address, addressExt);
            if (baseUserInfoResult != null && baseUserInfoResult.getUserId() != null) {
                userId = baseUserInfoResult.getUserId();
            }
        }

        if (userId != null && userId > 0L) {
//            BalanceTransferRequest balanceTransferRequest = BalanceTransferRequest
//                    .newBuilder()
//                    .setBalanceTransfer(BalanceTransferObj
//                            .newBuilder()
//                            .setAmount(amount)
//                            .setSourceUserId(header.getUserId())
//                            .setTargetUserId(userId)
//                            .setTokenId(tokenId)
//                            .setBusinessType(businessType)
//                            .build())
//                    .setHeader(HeaderConvertUtil.convertHeader(header))
//                    .setClientOrderId(clientOrderId)
//                    .build();
//
//            BalanceTransferResponse response
//                    = grpcAccountService.balanceTransfer(balanceTransferRequest);
            InstitutionalUserTransferRequest request = InstitutionalUserTransferRequest.newBuilder()
                    .setHeader(HeaderConvertUtil.convertHeader(header))
                    .setUserId(userId)
                    .setClientOrderId(clientOrderId)
                    .setTokenId(tokenId)
                    .setAmount(amount)
                    .setToLock(false)
                    .setBusinessType(businessType)
                    .setSubBusinessType(subBusinessType)
                    .build();
            InstitutionalUserTransferResponse response = grpcAccountService.institutionalUserTransfer(request);
            return TransferResult.builder().ret(response.getRet()).build();
        } else {
            throw new BrokerException(BrokerErrorCode.USER_NOT_EXIST);
        }
    }

    public BaseUserInfoResult getUserInfoByAddress(Header header, String tokenId, String address, String addressExt) {
        GetUserInfoByAddressResponse response = grpcOrderService.getUserInfoByAddress(GetUserInfoByAddressRequest
                .newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .setAddress(address)
                .setAddressExt(StringUtils.isEmpty(addressExt) ? "" : addressExt)
                .build());
        if (response == null || response.getUserId() == 0l) {
            return BaseUserInfoResult.builder().userId(0L).orgId(0L).accountId(0L).build();
        }
        return BaseUserInfoResult.builder().userId(response.getUserId()).orgId(response.getOrgId()).accountId(response.getAccountId()).build();
    }

    public DepositAddressResult beforeDeposit(Header header, String tokenId, String chainType) {
        GetDepositAddressRequest request = GetDepositAddressRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .setChainType(Strings.nullToEmpty(chainType))
                .build();
        GetDepositAddressResponse response = grpcAccountService.getDepositAddress(request);
        return DepositAddressResult.builder()
                .allowDeposit(response.getAllowDeposit())
                .address(response.getAllowDeposit() ? response.getAddress() : "")
                .addressExt(response.getAllowDeposit() ? Strings.nullToEmpty(response.getAddressExt()) : "")
                .minQuantity(response.getMinQuantity())
                .needAddressTag(response.getNeedAddressTag())
                .requiredConfirmNum(response.getRequiredConfirmNum())
                .canWithdrawConfirmNum(response.getCanWithdrawConfirmNum())
                .tokenType(response.getTokenType())
                .build();
    }

    public WithdrawResult openApiWithdraw(Header header,
                                          String clientOrderId,
                                          String address,
                                          String addressExt,
                                          String tokenId,
                                          String withdrawQuantity,
                                          String chainType,
                                          Boolean isAutoConvert,
                                          Boolean isQuick) {
        OpenApiWithdrawResponse response = grpcAccountService.openApiWithdraw(OpenApiWithdrawRequest
                .newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .setAddress(address)
                .setAddressExt(Strings.nullToEmpty(addressExt))
                .setWithdrawQuantity(withdrawQuantity)
                .setClientOrderId(clientOrderId)
                .setChainType(chainType)
                .setIsAutoConvert(isAutoConvert)
                .setIsQuick(isQuick)
                .build());
        if (!response.getAllowWithdraw()) {
            throw new OpenApiException(ApiErrorCode.WITHDRAW_NOT_ALLOW);
        }
        return WithdrawResult.builder()
                .success(Boolean.TRUE)
                .orderId(response.getWithdrawOrderId())
                .needBrokerAudit(response.getNeedBrokerAudit())
                .allowWithdraw(response.getAllowWithdraw())
                .refuseReason(String.valueOf(response.getRefuseWithdrawReason()))
                .build();
    }

    public List<DepositOrderResult> queryDepositOrder(Header header, String tokenId, Long fromOrderId,
                                                      Long startTime, Long endTime, Integer limit) {
        QueryDepositOrdersRequest request = QueryDepositOrdersRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .setFromId(fromOrderId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLimit(limit)
                .build();
        QueryDepositOrdersResponse response = grpcOrderService.queryDepositOrder(request);
        List<DepositOrderResult> depositOrderResultList = Lists.newArrayList();
        List<DepositOrder> depositOrderList = response.getOrdersList();
        if (depositOrderList != null && depositOrderList.size() > 0) {
            depositOrderResultList = depositOrderList.stream().map(this::getDepositOrderResult).collect(Collectors.toList());
        }
        return depositOrderResultList;
    }

    private DepositOrderResult getDepositOrderResult(DepositOrder order) {
//        String statusCode = order.getStatusCode();
//        String statusDesc = messageSource.getMessage("DepositStatus." + statusCode, null,
//                Objects.requireNonNull(StringUtils.parseLocale(header.getLanguage())));
        return DepositOrderResult.builder()
                .orderId(order.getOrderId())
                .token(order.getTokenId())
                .tokenName(order.getTokenName())
                .address(order.getAddress())
                .addressTag(order.getAddressExt())
                .fromAddress(order.getFromAddress())
                .fromAddressTag(order.getFromAddressExt())
                .quantity(order.getQuantity())
                .time(order.getTime())
                .status(order.getStatus())
                .statusCode(order.getStatusCode())
                .txid(order.getTxid())
                .txidUrl(order.getTxidUrl())
                .requiredConfirmNum(order.getRequiredConfirmNum())
                .confirmNum(order.getConfirmNum())
                .build();
    }

    public List<WithdrawDetailResult> queryWithdrawOrder(Header header, String tokenId, Long fromOrderId,
                                                         Long startTime, Long endTime, Integer limit) {
        QueryWithdrawOrdersRequest request = QueryWithdrawOrdersRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setTokenId(tokenId)
                .setFromId(fromOrderId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLimit(limit)
                .build();
        QueryWithdrawOrdersResponse response = grpcOrderService.queryWithdrawOrder(request);
        List<WithdrawDetailResult> depositOrderResultList = Lists.newArrayList();
        List<WithdrawOrder> depositOrderList = response.getOrdersList();
        if (depositOrderList != null && depositOrderList.size() > 0) {
            depositOrderResultList = depositOrderList.stream().map(this::getWithdrawOrderResult).collect(Collectors.toList());
        }
        return depositOrderResultList;
    }

    public WithdrawDetailResult getWithdrawOrderDetail(Header header,
                                                       String clientOrderId,
                                                       Long orderId) {
        GetWithdrawOrderDetailResponse response = grpcAccountService.getWithdrawOrderDetail(GetWithdrawOrderDetailRequest
                .newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setClientOrderId(clientOrderId)
                .setOrderId(orderId)
                .build());
        WithdrawOrder withdrawOrder = response.getOrder();
        return getWithdrawOrderResult(withdrawOrder);
    }

    private WithdrawDetailResult getWithdrawOrderResult(WithdrawOrder withdrawOrder) {
        return WithdrawDetailResult.builder()
                .orderId(withdrawOrder.getOrderId())
                .accountId(withdrawOrder.getAccountId())
                .token(withdrawOrder.getTokenId())
                .tokenId(withdrawOrder.getTokenId())
                .tokenName(withdrawOrder.getTokenName())
                .address(withdrawOrder.getAddress())
                .addressExt(withdrawOrder.getAddressExt())
                .quantity(withdrawOrder.getArriveQuantity())
                .arriveQuantity(withdrawOrder.getArriveQuantity())
                .status(withdrawOrder.getStatus())
                .statusCode(withdrawOrder.getStatusCode())
                .txid(withdrawOrder.getTxid())
                .txidUrl(withdrawOrder.getTxidUrl())
                .walletHandleTime(withdrawOrder.getWalletHandleTime())
                .time(withdrawOrder.getTime())
                .feeTokenId(withdrawOrder.getTotalFee().getFeeTokenId())
                .feeTokenName(withdrawOrder.getTotalFee().getFeeTokenName())
                .fee(withdrawOrder.getTotalFee().getFee())
                .requiredConfirmNum(withdrawOrder.getRequiredConfirmNum())
                .confirmNum(withdrawOrder.getConfirmNum())
                .kernelId(withdrawOrder.getKernelId())
                .isInternalTransfer(withdrawOrder.getIsInternalTransfer())
                .build();
    }
}
