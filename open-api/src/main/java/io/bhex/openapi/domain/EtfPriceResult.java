package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class EtfPriceResult {

    private Long exchangeId;

    private String symbolId;

    private String etfPrice;

    private String underlyingIndexId;

    private String underlyingPrice;

    private Long time;

    private String contractSymbolId;

    private Boolean isLong;

    private Integer leverage;
}
