package io.bhex.openapi.domain.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForBigDecimal;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractsResult {
    String symbol;
    String symbolName;
    String baseToken;
    String quoteToken;
    BigDecimal lastPrice;
    BigDecimal baseVolume;
    BigDecimal quoteVolume;
    BigDecimal bid;
    BigDecimal ask;
    BigDecimal high;
    BigDecimal low;
    String productType;
    String openInterest;
    BigDecimal indexPrice;
    String index;
    String indexBaseToken;
    Long startTs;
    Long endTs;
    BigDecimal fundingRate;
    BigDecimal nextFundingRate;
    Integer nextFundingRateTs;
}
