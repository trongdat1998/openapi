package io.bhex.openapi.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LotSize extends SymbolFilter {

    private String minQty;
    private String maxQty;
    private String stepSize;
}
