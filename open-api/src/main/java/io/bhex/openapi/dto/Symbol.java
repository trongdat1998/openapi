package io.bhex.openapi.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Symbol<T extends SymbolFilter> implements Serializable {

    private List<T> filters;

    private String exchangeId;
    private String symbol;
    private String symbolName;
    //SymbolStatus.toString()
    private String status;
    private String baseAsset;
    private String baseAssetName;
    private String baseAssetPrecision;
    private String quoteAsset;
    private String quoteAssetName;
    private String quotePrecision;
    private boolean icebergAllowed;
    private boolean isAggregate;
    private boolean allowMargin;


}
