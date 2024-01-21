package io.bhex.openapi.domain.futures;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class RiskLimit {
    private String riskLimitId;
    private String quantity;
    private String initialMargin;
    private String maintMargin;
}
