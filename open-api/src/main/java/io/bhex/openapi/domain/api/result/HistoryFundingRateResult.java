package io.bhex.openapi.domain.api.result;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class HistoryFundingRateResult {

    private Long id;

    private String symbol;

    // 资金费率结算时间
    private Long settleTime;
    // 资金费率
    private String settleRate;

}
