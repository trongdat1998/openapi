package io.bhex.openapi.domain.api.result;

import lombok.Builder;
import lombok.Data;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-10 14:20
 */
@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class MarginRepayOrderResult {
    private Long repayOrderId;
    private Long accountId;
    private String clientId;
    private String tokenId;
    private Long loanOrderId;
    private String amount;
    private String interest;
    private Long createAt;
}
