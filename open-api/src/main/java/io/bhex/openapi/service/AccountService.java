/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.service.impl
 *@Date 2018/6/10
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.service;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import io.bhex.base.account.BalanceDetail;
import io.bhex.base.account.BusinessSubject;
import io.bhex.base.proto.DecimalUtil;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.grpc.account.*;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.common.Platform;
import io.bhex.openapi.domain.*;
import io.bhex.openapi.domain.api.result.*;
import io.bhex.openapi.grpc.client.GrpcAccountService;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service("accountService")
public class AccountService {

    private static final String BALANCE_FLOW_TYPE_PREFIX = "BALANCE_FLOW_";

    private static final String BUY_OPTION = "BUY_OPTION";

    private static final String SELL_OPTION = "SELL_OPTION";

    @Resource
    private GrpcAccountService grpcAccountService;

    @Resource(name = "authorizeRedisTemplate")
    private RedisTemplate<String, Long> authorizeRedisTemplate;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private MessageSource messageSource;

    /**
     * get account info, include account balance
     */
    public AccountResult getAccount(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex) {
        GetAccountRequest request = GetAccountRequest.newBuilder()
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .build();
        GetAccountResponse response = grpcAccountService.getAccount(HeaderConvertUtil.convertHeader(header), request);
        return AccountResult.convert(response.getAccount());
    }

    public BalanceResult getBalance(BalanceDetail balanceDetail) {
        return BalanceResult.builder()
                .asset(balanceDetail.getToken().getTokenId())
                .free(DecimalUtil.toBigDecimal(balanceDetail.getAvailable()).stripTrailingZeros().toPlainString())
                .locked(DecimalUtil.toBigDecimal(balanceDetail.getLocked()).stripTrailingZeros().toPlainString())
                .build();
    }

    public SocketAccountInfo getSocketAccount(BalanceDetail balanceDetail) {
        return SocketAccountInfo.builder()
                .eventType("outboundAccountInfo")
                .eventTime(System.currentTimeMillis())
                .canTrade(true)
                .canWithdraw(true)
                .canDeposit(true)
                .balanceChangedList(Lists.newArrayList(SocketBalanceInfo.builder()
                        .asset(balanceDetail.getToken().getTokenId())
                        .free(DecimalUtil.toBigDecimal(balanceDetail.getAvailable()).stripTrailingZeros().toPlainString())
                        .locked(DecimalUtil.toBigDecimal(balanceDetail.getLocked()).stripTrailingZeros().toPlainString())
                        .build()))
                .build();
    }

    /**
     * get option account info
     */
    // TODO fix this
    public OptionAccountResult getOptionAccount(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex) {
//        GetAllAssetInfoResponse getAllAssetInfoResponse = grpcAccountService.getAllAssetInfo(
//                GetAllAssetInfoRequest.newBuilder()
//                        .setHeader(HeaderConvertUtil.convertHeader(header))
//                        .build());

        OptionAssetListResponse optionAssetListResponse = grpcAccountService.getOptionAssetList(OptionAssetRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .build());

        OptionAccountDetailListResponse optionAccountDetailListResponse
                = grpcAccountService.getOptionAccountDetail(OptionAccountDetailRequest
                .newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setAccountId(accountId)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .build());
        //组装数据
        return OptionAccountResult
                .builder()
//                .totalAsset(getAllAssetInfoResponse.getTotalAsset())
//                .optionAsset(getAllAssetInfoResponse.getOptionAsset())
                .optionAsset(optionAssetListResponse.getOptionTotalUsdtAsset())
                .balances(createOptionBalanceResult(optionAccountDetailListResponse))
                .build();
    }

    private List<OptionBalanceResult> createOptionBalanceResult(OptionAccountDetailListResponse response) {
        if (response == null || response.getOptionAccountDetailList().size() == 0) {
            return new ArrayList<>();
        }

        List<OptionBalanceResult> optionBalanceResults = new ArrayList<>();
        response.getOptionAccountDetailList().forEach(detail -> {
            optionBalanceResults.add(OptionBalanceResult
                    .builder()
                    .tokenName(detail.getTokenName())
                    .locked(detail.getLocked())
                    .margin(detail.getMargin())
                    .free(detail.getAvailable())
                    .build());
        });
        return optionBalanceResults;
    }

    public List<AccountResult> querySubAccount(Header header) {
        QueryAllSubAccountRequest request = QueryAllSubAccountRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .build();
        QuerySubAccountResponse response = grpcAccountService.queryAllSubAccount(request);
        return response.getSubAccountList().stream().map(this::getSubAccountResult).collect(Collectors.toList());
    }

    public List<BalanceFlowResult> queryBalanceFlow(Header header, String tokenId, Integer businessSubject,
                                                    Long fromFlowId, Long endFlowId, Long startTime, Long endTime, Integer limit,
                                                    AccountTypeEnum accountType, Integer accountIndex) {
        QueryBalanceFlowRequest request = QueryBalanceFlowRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .addTokenIds(tokenId)
                .addAllBalanceFlowTypes(businessSubject != null && businessSubject != 0 && businessSubject > 0 ? Lists.newArrayList(businessSubject) : Lists.newArrayList())
                .setFromId(fromFlowId)
                .setEndId(endFlowId)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setLimit(limit)
                .setAccountType(accountType)
                .setAccountIndex(accountIndex)
                .build();
        QueryBalanceFlowResponse response = grpcAccountService.queryBalanceFlow(request);
        List<BalanceFlowResult> balanceFlowResultList = Lists.newArrayList();
        if (response.getBalanceFlowsList() != null && response.getBalanceFlowsList().size() > 0) {
            balanceFlowResultList = response.getBalanceFlowsList().stream().map(this::getBalanceFlow).collect(Collectors.toList());
        }
        return balanceFlowResultList;
    }

    public BalanceFlowResult getBalanceFlow(BalanceFlow balanceFlow) {
        String flowName = balanceFlow.getFlowName();
        if (Strings.isNullOrEmpty(flowName)) {
            String flowType = BALANCE_FLOW_TYPE_PREFIX + BusinessSubject.forNumber(balanceFlow.getBusinessSubject()).name();
            // 业务需要，在流水页面展示交易记录。增加显示为"卖出期权" 减少为"买入期权"
            if (BusinessSubject.TRADE.toString().equals(balanceFlow.getBalanceFlowType())) {
                flowType = new BigDecimal(balanceFlow.getChanged()).compareTo(BigDecimal.ZERO) > 0 ? BALANCE_FLOW_TYPE_PREFIX + SELL_OPTION : BALANCE_FLOW_TYPE_PREFIX + BUY_OPTION;
            }
            flowName = messageSource.getMessage(flowType, null, "", LocaleContextHolder.getLocale());
        }
        return BalanceFlowResult.builder()
                .id(balanceFlow.getFlowId())
                .accountId(balanceFlow.getAccountId())
                .token(balanceFlow.getTokenId())
                .tokenId(balanceFlow.getTokenId())
                .tokenName(balanceFlow.getTokenName())
                .flowTypeValue(balanceFlow.getBusinessSubject())
                .flowType(balanceFlow.getBalanceFlowType())
                .flowName(flowName)
                .change(balanceFlow.getChanged())
                .total(balanceFlow.getTotal())
                .created(balanceFlow.getCreated())
                .build();
    }

    public void subAccountTransfer(Header header, Integer fromAccountType, Integer fromAccountIndex, Integer toAccountType, Integer toAccountIndex,
                                   String token, String amount) {
        SubAccountTransferRequest request = SubAccountTransferRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setFromAccountType(AccountType.toAccountTypeEnum(fromAccountType))
                .setFromIndex(fromAccountIndex)
                .setToAccountType(AccountType.toAccountTypeEnum(toAccountType))
                .setToIndex(toAccountIndex)
                .setToken(token)
                .setAmount(new BigDecimal(amount).stripTrailingZeros().toPlainString())
                .build();
        grpcAccountService.subAccountTransfer(request);
    }

    public AccountResult getSubAccountResult(SubAccount subAccount) {
        return AccountResult.builder()
                .accountId(subAccount.getAccountId())
                .accountName(subAccount.getAccountName())
                .accountType(subAccount.getAccountType())
                .accountIndex(subAccount.getIndex())
                .build();
    }

    public Long createThirdPartyAccount(Header header, String thirdUserId) {
        CreateThirdPartyAccountResponse response = grpcAccountService.createThirdPartyAccount(CreateThirdPartyAccountRequest
                .newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setThirdUserId(thirdUserId)
                .build());
        return response.getUserId();
    }

    public ThirdPartyUserTokenResult createThirdPartyToken(Header header, String thirdUserId, Long userId, String requestPlatform, Integer accountType) {
        CreateThirdPartyTokenResponse response = grpcAccountService.createThirdPartyToken(CreateThirdPartyTokenRequest
                .newBuilder()
//                .setHeader(HeaderConvertUtil.convertHeader(header).toBuilder().setPlatform(Platform.valueOf(requestPlatform)).build())
                .setHeader(HeaderConvertUtil.convertHeader(header).toBuilder().setPlatform(Platform.valueOf(requestPlatform)).build())
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .setAccountType(accountType)
                .build());
        handleLoginToken(requestPlatform, header.getOrgId(), response.getUserId(), response.getAuToken());
        return ThirdPartyUserTokenResult
                .builder()
//                .result(Boolean.TRUE)
//                .msg("success")
                .userId(response.getUserId())
                .auToken(response.getAuToken())
                .expireTime(Long.parseLong(String.valueOf(BrokerConstants.DEFAULT_TOKEN_EFFECTIVE_SECONDS)))
                .accountId(response.getAccountId())
                .build();
    }

    private void handleLoginToken(String loginPlatform, Long orgId, Long userId, String newToken) {
        String md5Token = Hashing.md5().hashString(newToken, Charsets.UTF_8).toString();
        log.info("orgId:{}, userId:{}, tokenMd5:{}", orgId, userId, md5Token);
        authorizeRedisTemplate.opsForValue().set(md5Token, userId, BrokerConstants.DEFAULT_TOKEN_EFFECTIVE_SECONDS, TimeUnit.SECONDS);
        String currentTokenKey = String.format(BrokerConstants.CURRENT_AU_TOKEN, loginPlatform, orgId, userId);
        String oldMd5Token = redisTemplate.opsForValue().getAndSet(currentTokenKey, md5Token);
        if (!Strings.isNullOrEmpty(oldMd5Token) && !oldMd5Token.equals(md5Token)) {
            authorizeRedisTemplate.delete(oldMd5Token);
            redisTemplate.convertAndSend(BrokerConstants.LOGIN_NOTICE_CHANNEL, oldMd5Token);
        }
    }

    public void clearToken(Header header, String thirdUserId, Long userId, String requestPlatform) {
        GetThirdPartyAccountResponse response = grpcAccountService.getThirdPartyAccount(GetThirdPartyAccountRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header).toBuilder().setPlatform(Platform.valueOf(requestPlatform)).build())
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .build());
        //清理token
        cleanToken(header, response.getUserId(), requestPlatform);
    }

    /**
     * 需要orgId userId platform
     *
     * @param header
     * @param userId
     * @param requestPlatform
     */
    public void cleanToken(Header header, Long userId, String requestPlatform) {
        if (header != null && userId != null && userId > 0) {
            String currentTokenKey = String.format(BrokerConstants.CURRENT_AU_TOKEN, requestPlatform, header.getOrgId(), userId);
            String md5Token = redisTemplate.opsForValue().get(currentTokenKey);
            if (!Strings.isNullOrEmpty(md5Token)) {
                redisTemplate.delete(currentTokenKey);
                authorizeRedisTemplate.delete(md5Token);
                redisTemplate.convertAndSend(BrokerConstants.LOGOUT_NOTICE_CHANNEL, md5Token);
            }
        }
    }

    public void transferIn(Header header, String thirdUserId, Long userId, AccountTypeEnum accountTypeEnum,
                           String clientOrderId, String tokenId, String amount) {
        ThirdPartyUserTransferInRequest request = ThirdPartyUserTransferInRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .setAccountType(accountTypeEnum)
                .setClientOrderId(clientOrderId)
                .setTokenId(tokenId)
                .setAmount(amount)
                .build();
        grpcAccountService.thirdPartyUserTransferIn(request);
    }

    public void transferOut(Header header, String thirdUserId, Long userId, AccountTypeEnum accountTypeEnum,
                            String clientOrderId, String tokenId, String amount) {
        ThirdPartyUserTransferOutRequest request = ThirdPartyUserTransferOutRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .setAccountType(accountTypeEnum)
                .setClientOrderId(clientOrderId)
                .setTokenId(tokenId)
                .setAmount(amount)
                .build();
        grpcAccountService.thirdPartyUserTransferOut(request);
    }

    public List<BalanceResult> getThirdUserBalance(Header header, String thirdUserId, Long userId, AccountTypeEnum accountTypeEnum) {
        ThirdPartyUserBalanceRequest request = ThirdPartyUserBalanceRequest.newBuilder()
                .setHeader(HeaderConvertUtil.convertHeader(header))
                .setThirdUserId(thirdUserId)
                .setUserId(userId)
                .setAccountType(accountTypeEnum)
                .build();
        ThirdPartyUserBalanceResponse response = grpcAccountService.thirdPartyUserBalance(request);
        return response.getBalancesList().stream()
                .map(balance -> BalanceResult.builder()
                        .asset(balance.getTokenId())
                        .total(balance.getTotal())
                        .free(balance.getFree())
                        .locked(balance.getLocked())
                        .build())
                .collect(Collectors.toList());
    }

}
