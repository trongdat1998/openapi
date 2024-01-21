package io.bhex.openapi.domain.api.result;

import lombok.Builder;
import lombok.Data;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-10 10:44
 */
@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class MarginLoanPositionResult {

    private String tokenId;
    private String loanTotal;
    private String loanBtcValue;
    private String interestPaid;
    private String interestUnpaid;
    private String unpaidBtcValue;
}
