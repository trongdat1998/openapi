package io.bhex.openapi.domain.api.result;

import lombok.Builder;
import lombok.Data;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-09-21 11:31
 */
@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class MarginInterestResult {
    private Long orgId;
    private String tokenId;
    private String interest;
    private int interestPeriod;
    private int calculationPeriod;
    private int settlementPeriod;
    private Long levelConfigId;
    private Long created;
    private Long updated;
}
