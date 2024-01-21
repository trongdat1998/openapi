package io.bhex.openapi.domain.finance_support;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class FinanceAccountResult {
    private Long id;
    private Long orgId;
    private Long userId;
    private Long accountId;
    private Integer accountType;
    private Integer createType;
    private Integer accountIndex;
}
