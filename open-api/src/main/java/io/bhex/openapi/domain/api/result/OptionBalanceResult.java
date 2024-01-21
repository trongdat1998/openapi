package io.bhex.openapi.domain.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OptionBalanceResult {

    private String tokenName;

    private String free;

    private String locked;

    private String margin;

}
