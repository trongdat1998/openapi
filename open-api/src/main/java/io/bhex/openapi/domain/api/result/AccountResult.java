package io.bhex.openapi.domain.api.result;

import io.bhex.broker.grpc.account.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountResult {

    private Long accountId;
    private String accountName;
    private Integer accountType;
    private Integer accountIndex;
    private Boolean canTrade;
    private Boolean canDeposit;
    private Boolean canWithdraw;
    private List<BalanceResult> balances;
    private Long updateTime;
    public static AccountResult convert(Account account) {
        return AccountResult.builder()
//                .canDeposit(account.getCanDeposit())
//                .canWithdraw(account.getCanDeposit())
//                .canTrade(account.getCanTrade())
                .balances(account.getBalanceList().stream().map(BalanceResult::convert).collect(Collectors.toList()))
//                .updateTime()
                .build();
    }
}
