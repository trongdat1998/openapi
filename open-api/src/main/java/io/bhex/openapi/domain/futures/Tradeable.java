package io.bhex.openapi.domain.futures;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tradeable {

    private String avaiable;
    private String margin;
    private String marginLocked;
    private String marginRate;
    private String unrealizedPnL;
    private String realizedPnL;
}
