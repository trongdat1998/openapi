package io.bhex.openapi.domain.api.result;

import io.bhex.openapi.domain.futures.RiskLimit;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class TokenFuturesResult {

    private String symbol;
    private Boolean inverse;
    private String index;
    private List<RiskLimit> riskLimits;
}
