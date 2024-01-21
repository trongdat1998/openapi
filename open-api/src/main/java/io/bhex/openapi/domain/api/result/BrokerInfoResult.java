package io.bhex.openapi.domain.api.result;

import io.bhex.openapi.domain.TokenResult;
import io.bhex.openapi.dto.FuturesSymbol;
import io.bhex.openapi.dto.RateLimit;
import io.bhex.openapi.dto.Symbol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BrokerInfoResult {

    private String timezone;

    private Long serverTime;

    private List brokerFilters;

    private List<Symbol> symbols;

//    private List<Symbol> aggregates;

    private List<RateLimit> rateLimits;

    private List<Symbol> options;

    private List<FuturesSymbol> contracts;

    private List<TokenResult> tokens;

}
