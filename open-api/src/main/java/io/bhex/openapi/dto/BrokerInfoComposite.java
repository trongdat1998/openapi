package io.bhex.openapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * broker组合信息
 */
@Getter
@Setter
public class BrokerInfoComposite implements Serializable {

     private List brokerFilters;
     private List<Symbol> symbols;
     private String timezone;
     private long serverTime;
     private List<RateLimit> rateLimits;


}
