package io.bhex.openapi.service;

import com.google.common.collect.ImmutableMap;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.finance_support.*;
import io.bhex.openapi.domain.finance_support.FinanceAccountResult;
import io.bhex.openapi.grpc.client.GrpcFinanceSupportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FinanceSupportService {

    private static ImmutableMap<Long, FinanceAccountInfo> financeAccountMap = ImmutableMap.of();

    @Resource
    private GrpcFinanceSupportService grpcFinanceSupportService;

    @PostConstruct
    @Scheduled(cron = "0 0/5 * * * ?")
    private void refreshFinanceAccouts() {
        Map<Long, FinanceAccountInfo> financeAccountMapTmp = new HashMap<>();
        QueryFinanceAccountsRequest request = QueryFinanceAccountsRequest.newBuilder().build();
        QueryFinanceAccountsResponse response = grpcFinanceSupportService.queryFinanceAccounts(request);
        log.info("refreshFinanceAccouts: query account size: {}", response.getAccountInfoCount());
        for (FinanceAccountInfo accountInfo : response.getAccountInfoList()) {
            financeAccountMapTmp.put(accountInfo.getOrgId(), accountInfo);
        }

        financeAccountMap = ImmutableMap.copyOf(financeAccountMapTmp);
    }

    /**
     * 根据券商机构ID创建财务账号
     * 注：一个机构最多只能有一个财务账号
     *
     * @param orgId 券商机构ID
     * @return accountId
     */
    public FinanceAccountResult createFinanceAccount(Long userId, Long orgId) {
        CreateFinanceAccountRequest request = CreateFinanceAccountRequest.newBuilder()
                .setUserId(userId)
                .setOrgId(orgId)
                .setAccountType(AccountTypeEnum.COIN)
                .build();
        CreateFinanceAccountResponse response = grpcFinanceSupportService.createFinanceAccount(request);
        return toFinanceAccountResult(response.getAccountInfo());
    }

    public List<FinanceAccountResult> getAllFinanceAccounts() {
        return financeAccountMap.values().stream().map(this::toFinanceAccountResult).collect(Collectors.toList());
    }

    public FinanceAccountResult getFinanceAccount(Long orgId, Long accountId) {
        QueryFinanceAccountsRequest.Builder requestBuilder = QueryFinanceAccountsRequest.newBuilder();
        if (orgId != null) {
            requestBuilder.setOrgId(orgId);
        }

        if (accountId != null) {
            requestBuilder.setAccountId(accountId);
        }

        QueryFinanceAccountsResponse response = grpcFinanceSupportService.queryFinanceAccounts(requestBuilder.build());
        if (response.getAccountInfoCount() != 0) {
            return toFinanceAccountResult(response.getAccountInfo(0));
        }

        return null;
    }

    /**
     * 判断财务账户是否有效
     *
     * @param accountId 账户ID
     * @return true-有效 false-无效
     */
    public boolean isValidFinanceAccount(Long orgId, Long accountId) {
        FinanceAccountInfo accountInfo = financeAccountMap.get(orgId);
        return accountInfo != null && accountInfo.getAccountId() == accountId;
    }

    /**
     * 财务内部账户转账
     *
     * @param sourceAccountId 源账户ID
     * @param sourceOrgId 源券商机构ID
     * @param targetAccountId 目标账户ID
     * @param targetOrgId 目标机券商构ID
     * @param amount 转账金额
     * @param clientTransferId 客户端唯一转账标识
     */
    public void transfer(Long sourceAccountId, Long sourceOrgId, Long targetAccountId,
                            Long targetOrgId, String amount, String tokenId, Long clientTransferId) {
        FinanceAccountTransferRequest request = FinanceAccountTransferRequest.newBuilder()
                .setSourceAccountId(sourceAccountId)
                .setSourceOrgId(sourceOrgId)
                .setTargetAccountId(targetAccountId)
                .setTargetOrgId(targetOrgId)
                .setAmount(amount)
                .setTokenId(tokenId)
                .setClientTransferId(clientTransferId)
                .build();

        grpcFinanceSupportService.financeAccountTransfer(request);
    }

    private FinanceAccountResult toFinanceAccountResult(FinanceAccountInfo accountInfo) {
        if (accountInfo == null) {
            return FinanceAccountResult.builder().build();
        }

        return FinanceAccountResult.builder()
                .id(accountInfo.getId())
                .accountId(accountInfo.getAccountId())
                .userId(accountInfo.getUserId())
                .orgId(accountInfo.getOrgId())
                .accountType(accountInfo.getAccountType().getNumber())
                .createType(accountInfo.getCreateType().getNumber())
                .accountIndex(accountInfo.getAccountIndex())
                .build();
    }
}
