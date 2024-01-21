package io.bhex.openapi.domain.api.result;

import io.bhex.openapi.domain.futures.Tradeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FuturesAccountResult {
    private String total;
    private String availableMargin;
    private String positionMargin;
    private String orderMargin;
    private String tokenId;
}
