package io.bhex.openapi.domain.api.result;

import lombok.Builder;
import lombok.Data;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-10 10:28
 */
@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class MarginTokenResult {

    private Long exchangeId;
    private String tokenId;
    private String convertRate;
    private Integer leverage;
    private boolean canBorrow;
    private String maxQuantity;
    private String minQuantity;
    private String quantityPrecision;
    private String repayMinQuantity;
    private String interest;
    private Integer isOpen;
}
