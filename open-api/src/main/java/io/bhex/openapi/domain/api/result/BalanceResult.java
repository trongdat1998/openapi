package io.bhex.openapi.domain.api.result;

import io.bhex.broker.grpc.account.Balance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BalanceResult {

    private String asset;
    private String assetId;
    private String assetName;
    private String total;
    private String free;
    private String locked;

    public static BalanceResult convert(Balance balance) {
        return BalanceResult.builder()
                .asset(balance.getTokenId())
                .assetId(balance.getTokenId())
                .assetName(balance.getTokenName())
                .total(balance.getTotal())
                .free(balance.getFree())
                .locked(balance.getLocked())
                .build();
    }
}
