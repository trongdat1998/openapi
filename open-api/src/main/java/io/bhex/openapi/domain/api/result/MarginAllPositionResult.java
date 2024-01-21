package io.bhex.openapi.domain.api.result;

import lombok.Builder;
import lombok.Data;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-10 11:51
 */
@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class MarginAllPositionResult {
    private String total;
    private String loanAmount;
    private String availMargin;
    private String occupyMargin;

}
