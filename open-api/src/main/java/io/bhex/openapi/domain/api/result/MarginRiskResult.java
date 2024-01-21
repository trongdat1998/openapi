package io.bhex.openapi.domain.api.result;

import lombok.Builder;
import lombok.Data;

/**
 * @author JinYuYuan
 * @description
 * @date 2020-08-10 10:37
 */
@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class MarginRiskResult {
    private String withdrawLine;
    private String warnLine;
    private String appendLine;
    private String stopLine;
}
