package io.bhex.openapi.dto;

import io.bhex.openapi.domain.futures.RiskLimit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FuturesSymbol<T extends SymbolFilter> implements Serializable {
    private List<T> filters;

    private String exchangeId;
    private String symbol;
    private String symbolName;
    private String status;
    private String baseAsset;
    private String baseAssetPrecision;
    private String quoteAsset;
    private String quoteAssetPrecision;
    private boolean icebergAllowed;
    private Boolean inverse;
    private String index;
    private String marginToken;
    private String marginPrecision;
    private String contractMultiplier;
    private String underlying;
    private List<RiskLimit> riskLimits;
}
