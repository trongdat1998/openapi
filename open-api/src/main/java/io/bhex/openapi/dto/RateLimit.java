package io.bhex.openapi.dto;

import io.bhex.openapi.domain.api.enums.RateLimitInterval;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RateLimit implements Serializable {

    // 限制类型
    private RateLimitType rateLimitType;
    // 限制周期
    private RateLimitInterval interval;
    // 周期单位
    private int intervalUnit;
    // 限制次数
    private int limit;
}
