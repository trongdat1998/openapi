package io.bhex.openapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceFilter extends SymbolFilter {

    private String minPrice;
    private String maxPrice;
    private String tickSize;
}
