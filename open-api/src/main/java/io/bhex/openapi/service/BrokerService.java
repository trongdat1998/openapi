package io.bhex.openapi.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.bhex.broker.common.entity.Header;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.SymbolResult;
import io.bhex.openapi.domain.TokenOptionResult;
import io.bhex.openapi.domain.TokenResult;
import io.bhex.openapi.domain.api.enums.ApiTradeType;
import io.bhex.openapi.domain.api.enums.SymbolStatus;
import io.bhex.openapi.domain.api.result.BrokerInfoResult;
import io.bhex.openapi.domain.api.result.ContractsResult;
import io.bhex.openapi.domain.api.result.PairsResult;
import io.bhex.openapi.domain.api.result.TokenFuturesResult;
import io.bhex.openapi.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BrokerService {

    @Autowired
    BasicService basicService;

    @Autowired
    RateLimitService rateLimitService;

    public BrokerInfoResult getBrokerInfo(Header header, String tradeType) {

        // 获取币种对
        List<SymbolResult> symbolResultList = basicService.querySymbolList(header);

        // 获取Tokens
        List<TokenResult> tokenResultList = basicService.queryTokenList(header);

        // 获取服务器时间
        Long serverTime = basicService.getServerTime(header, 0L);

        // 获取限制规则
        List<RateLimit> rateLimits = getRateLimits(header.getOrgId());

        List<SymbolResult> symbols
                = symbolResultList.stream().filter(s -> s.getCategory() == 1).collect(Collectors.toList());

        List<SymbolResult> options
                = symbolResultList.stream().filter(s -> s.getCategory() == 3).collect(Collectors.toList());

        List<SymbolResult> contracts
                = symbolResultList.stream().filter(s -> s.getCategory() == 4).collect(Collectors.toList());

        /*List<SymbolResult> margins
                = symbolResultList.stream().filter(s -> s.getCategory() == 1)
                .filter(s -> s.getAllowMargin() ).collect(Collectors.toList());*/

        // 封装币种对
        List<Symbol> symbolList = packageSymbolList(symbols);
        // 期权币种对
        List<Symbol> optionList = packageSymbolList(options);
        // 合约币种对
        List<FuturesSymbol> contractList = packageFuturesSymbolList(contracts);
        /*// 杠杆
        List<Symbol> marginList = packageSymbolList(margins);*/

        BrokerInfoResult.BrokerInfoResultBuilder builder = BrokerInfoResult.builder()
                .timezone("UTC")
                .serverTime(serverTime)
                .brokerFilters(Lists.newArrayList())
                .rateLimits(rateLimits)
                .tokens(tokenResultList);

        if (StringUtils.isEmpty(tradeType)) {
            return builder
                    .symbols(symbolList)
//                    .aggregates(aggregateList)
                    .options(optionList)
                    .contracts(contractList)
                    .build();
        } else {
            try {
                ApiTradeType apiTradeType = ApiTradeType.valueOf(tradeType.toUpperCase());
                switch (apiTradeType) {
                    case TOKEN:
                        return builder.symbols(symbolList).build();
                    case OPTIONS:
                        return builder.options(optionList).build();
                    case CONTRACTS:
                        return builder.contracts(contractList).build();
                    default:
                        return builder.build();
                }
            } catch (IllegalArgumentException e) {
                throw new OpenApiException(ApiErrorCode.INVALID_PARAMETER, "type");
            }
        }

//        return BrokerInfoResult.builder()
//                .timezone("UTC")
//                .serverTime(serverTime)
//                .symbols(symbolList)
//                .options(optionList)
//                .contracts(contractList)
//                .brokerFilters(Lists.newArrayList())
//                .rateLimits(rateLimits)
//                .build();
    }


    public List<RateLimit> getRateLimits(Long brokerId) {
        return rateLimitService.getAllRateLimit();
    }

    public List<Symbol> packageSymbolList(List<SymbolResult> symbolResultList) {
        List<Symbol> list = Lists.newArrayList();
        if (CollectionUtils.isEmpty(symbolResultList)) {
            return list;
        }

        for (SymbolResult result : symbolResultList) {
            if (result.getHideFromOpenapi()) {
                continue;
            }
            List<SymbolFilter> filterList = buildSymbolFilters(result);
            Symbol symbol = Symbol.builder()
                    .exchangeId(result.getExchangeId().toString())
                    .symbol(result.getSymbolId())
                    .symbolName(result.getSymbolName())
                    .status(SymbolStatus.TRADING.toString())
                    .baseAsset(result.getBaseTokenId())
                    .baseAssetName(result.getBaseTokenName())
                    .baseAssetPrecision(result.getBasePrecision())
                    .quoteAsset(result.getQuoteTokenId())
                    .quoteAssetName(result.getQuoteTokenName())
                    .quotePrecision(result.getQuotePrecision())
                    .icebergAllowed(false)
                    .filters(filterList)
                    .allowMargin(result.getAllowMargin())
                    .isAggregate(result.getIsAggregate())
                    .build();
            list.add(symbol);
        }

        return list;
    }

    private List<FuturesSymbol> packageFuturesSymbolList(List<SymbolResult> symbolResultList) {
        List<FuturesSymbol> list = Lists.newArrayList();
        if (CollectionUtils.isEmpty(symbolResultList)) {
            return list;
        }

        for (SymbolResult result : symbolResultList) {
            if (result.getHideFromOpenapi()) {
                continue;
            }
            List<SymbolFilter> filterList = buildSymbolFilters(result);

            // 获取期货的SymbolResult
            SymbolResult futuresResult = basicService.getFuturesSymbolResult(
                    result.getOrgId(), null, result.getSymbolId());
            if (futuresResult == null) {
                log.warn("Can not find futures symbol result. orgId: {} symbolId: {}",
                        result.getOrgId(), result.getSymbolId());
                continue;
            }
            FuturesSymbol symbol = FuturesSymbol.builder()
                    .exchangeId(futuresResult.getExchangeId().toString())
                    .symbol(futuresResult.getSymbolId())
                    .symbolName(futuresResult.getSymbolName())
                    .status(SymbolStatus.TRADING.toString())
                    .baseAsset(futuresResult.getBaseTokenId())
                    .baseAssetPrecision(futuresResult.getBasePrecision())
                    .quoteAsset(futuresResult.getTokenFutures().getDisplayTokenId())
                    .quoteAssetPrecision(futuresResult.getMinPricePrecision())
                    .inverse(futuresResult.getIsReverse())
                    .riskLimits(futuresResult.getTokenFutures().getRiskLimits())
                    .index(futuresResult.getTokenFutures().getDisplayIndexToken())
                    .marginToken(futuresResult.getQuoteTokenId())
                    .marginPrecision(futuresResult.getTokenFutures().getMarginPrecision())
                    .contractMultiplier(futuresResult.getTokenFutures().getContractMultiplier())
                    .underlying(futuresResult.getTokenFutures().getDisplayUnderlyingId())
                    .icebergAllowed(false)
                    .filters(filterList)
                    .build();
            list.add(symbol);
        }

        return list;
    }

    private List<SymbolFilter> buildSymbolFilters(SymbolResult input) {

        List<SymbolFilter> list = Lists.newArrayList();

        if (!Strings.isNullOrEmpty(input.getMinPricePrecision())) {
            PriceFilter pf = new PriceFilter();
            pf.setMinPrice(input.getMinPricePrecision());
            pf.setTickSize(input.getMinPricePrecision());
            pf.setMaxPrice("100000.00000000");
            pf.setFilterType("PRICE_FILTER");
            list.add(pf);
        }

        if (!Strings.isNullOrEmpty(input.getMinTradeQuantity()) && !Strings.isNullOrEmpty(input.getBasePrecision())) {
            LotSize ls = new LotSize();
            ls.setMaxQty("100000.00000000");
            ls.setMinQty(input.getMinTradeQuantity());
            ls.setStepSize(input.getBasePrecision());
            ls.setFilterType("LOT_SIZE");
            list.add(ls);
        }

        if (!Strings.isNullOrEmpty(input.getMinTradeAmount())) {
            MinNotional mn = new MinNotional();
            mn.setMinNotional(input.getMinTradeAmount());
            mn.setFilterType("MIN_NOTIONAL");
            list.add(mn);
        }

        return list;
    }

    public List<TokenOptionResult> getOptionTokens(Header header, boolean expired) {
        return basicService.getOptionTokens(header, expired);
    }

    public List<TokenFuturesResult> getFuturesTokens(Header header, boolean expired) {
        return basicService.getFuturesTokens(header, expired);
    }

    public List<PairsResult> getPairs(Header header, boolean expired) {
        return basicService.querySymbolList(header).stream()
                .filter(symbolResult -> !symbolResult.getHideFromOpenapi())
                .filter(s -> s.getCategory() == 1).map(symbolResult -> {
                    return PairsResult.builder()
                            .baseToken(symbolResult.getBaseTokenName())
                            .quoteToken(symbolResult.getQuoteTokenName())
                            .symbol(symbolResult.getSymbolName())
                            .build();
                }).collect(Collectors.toList());
    }

    public List<ContractsResult> getContracts(Header header, boolean expired) {
        return basicService.getContracts(header, expired);
    }

}