package io.bhex.openapi.domain.api.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OptionAccountResult {

    private String totalAsset; //USDT

    private String optionAsset; //USDT

    private List<OptionBalanceResult> balances;

}
