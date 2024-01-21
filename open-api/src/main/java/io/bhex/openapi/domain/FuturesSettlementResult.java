package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class FuturesSettlementResult {

    private String symbol;
    private Long settlementTime;
    private String avgPrice;
    private String position;
    private String settlePrice;
    private String settlementPnL;
    private String realizedPnL;
    private String sessionPnL;
}
