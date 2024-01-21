package io.bhex.openapi.domain.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FundingRateResult {

    private String symbol;
    private Long intervalStart;
    private Long intervalEnd;
    private String rate;
    private String index;
}
