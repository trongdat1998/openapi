package io.bhex.openapi.domain.api.result;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class ModifyMarginResult {
    private String symbol;
    private String margin;
    private Long timestamp;
}
