package io.bhex.openapi.domain.api.result;

import lombok.Builder;
import lombok.Data;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-10 14:02
 */
@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class MarginLoanOrderResult {
    private Long loanOrderId;
    private String clientId;
    private String tokenId;
    private String loanAmount;
    private String repaidAmount;
    private String unpaidAmount;
    private String interestRate;
    private Long interestStart;
    private Integer status;
    private String interestPaid;
    private String interestUnpaid;
    private Long createAt;
    private Long updateAt;
    private Long accountId;


}