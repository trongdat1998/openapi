package io.bhex.openapi.domain;

import io.bhex.openapi.domain.api.enums.ApiFuturesPositionSide;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class FuturesPositionResult {

    private String symbol;
    private ApiFuturesPositionSide side;
    private String avgPrice;
    private String position;
    private String available;
    private String leverage;
    private String lastPrice;
    private String positionValue;
    private String flp;
    private String margin;
    private String marginRate;
    private String unrealizedPnL;
    private String profitRate;
    private String realizedPnL;
    // 当前这个字段只支持做市账号
    private String accountId;
}
