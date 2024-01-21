package io.bhex.openapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


//todo 有继承关系

/**
 "filters": [{
 "filterType": "PRICE_FILTER",
 "minPrice": "0.00000100",
 "maxPrice": "100000.00000000",
 "tickSize": "0.00000100"
 }, {
 "filterType": "LOT_SIZE",
 "minQty": "0.00100000",
 "maxQty": "100000.00000000",
 "stepSize": "0.00100000"
 }, {
 "filterType": "MIN_NOTIONAL",
 "minNotional": "0.00100000"
 }]
 */
@Setter
@Getter
public class SymbolFilter implements Serializable {

    private String filterType;

}
