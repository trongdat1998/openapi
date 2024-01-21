package io.bhex.openapi.util;

import io.bhex.base.proto.DecimalUtil;
import io.bhex.broker.grpc.basic.FuturesRiskLimit;
import io.bhex.broker.grpc.basic.Symbol;
import io.bhex.broker.grpc.basic.TokenFuturesInfo;
import io.bhex.broker.grpc.order.*;
import io.bhex.broker.grpc.order.OrderSide;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.*;
import io.bhex.openapi.domain.api.enums.ApiFuturesOrderType;
import io.bhex.openapi.domain.api.enums.ApiFuturesPositionSide;
import io.bhex.openapi.domain.api.enums.ApiFuturesPriceType;
import io.bhex.openapi.domain.api.result.FundingRateResult;
import io.bhex.openapi.domain.api.result.FuturesAccountResult;
import io.bhex.openapi.domain.api.result.InsuranceFundResult;
import io.bhex.openapi.domain.api.result.TokenFuturesResult;
import io.bhex.openapi.domain.futures.RiskLimit;
import io.bhex.openapi.domain.futures.Tradeable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.bhex.openapi.domain.BrokerConstants.FUTURES_AMOUNT_PRECISION;

public class FuturesTools {

    private static final long MILLSECONDS_WITH_8_HOUR = 8 * 3600 * 1000;

    public static TokenFuturesResult toFuturesTokenResult(SymbolResult symbolResult) {
        List<RiskLimit> riskLimits = symbolResult.getTokenFutures().getRiskLimits();
        return TokenFuturesResult.builder()
                .inverse(symbolResult.getIsReverse())
                .symbol(symbolResult.getSymbolId())
                .index(symbolResult.getTokenFutures().getDisplayIndexToken())
                .riskLimits(riskLimits)
                .build();
    }

    public static TokenFutures toTokenFutures(Symbol symbol) {
        TokenFuturesInfo t = symbol.getTokenFutures();
        return TokenFutures.builder()
                .tokenId(t.getTokenId())
                .displayTokenId(t.getDisplayTokenId())
                .issueDate(t.getIssueDate())
                .settlementDate(t.getSettlementDate())
                .currency(t.getCurrency())
                .currencyDisplay(t.getCurrencyDisplay())
                .contractMultiplier(t.getContractMultiplier())
                .maxLeverage(t.getMaxLeverage())
                .coinToken(t.getCoinToken())
                .limitDownInTradingHours(t.getLimitDownInTradingHours())
                .limitUpInTradingHours(t.getLimitUpInTradingHours())
                .limitDownOutTradingHours(t.getLimitDownOutTradingHours())
                .limitUpOutTradingHours(t.getLimitUpOutTradingHours())
                .riskLimits(FuturesTools.toRiskLimits(t.getRiskLimitsList()))
                .levers(Arrays.asList(t.getLeverageRange().split(",")))
                .overPriceRange(Arrays.asList(t.getOverPriceRange().split(",")))
                .marketPriceRange(Arrays.asList(t.getMarketPriceRange().split(",")))
                .marginPrecision(t.getMarginPrecision())
                .indexToken(symbol.getIndexToken())
                .displayIndexToken(symbol.getDisplayIndexToken())
                .displayUnderlyingId(t.getDisplayUnderlyingId())
                .build();
    }

    public static FuturesPositionResult toFuturesPositionResult(FuturesPosition position) {
        return FuturesPositionResult.builder()
                .symbol(position.getTokenId())
                .side(position.getIsLong().equals("1") ? ApiFuturesPositionSide.LONG : ApiFuturesPositionSide.SHORT)
                .avgPrice(position.getAvgPrice())
                .position(position.getTotal())
                .available(position.getAvailable())
                .leverage(position.getLeverage())
                .lastPrice(position.getIndices())
                .positionValue(position.getPositionValues())
                .flp(position.getLiquidationPrice())
                .margin(position.getMargin())
                .marginRate(position.getMarginRate())
                .unrealizedPnL(position.getUnrealisedPnl())
                .profitRate(position.getProfitRate())
                .realizedPnL(position.getRealisedPnl())
                .build();
    }

    public static FuturesSettlementResult toFuturesSettlementResult(FuturesSettlement futuresSettlement) {
        return FuturesSettlementResult.builder()
                .symbol(futuresSettlement.getSymbolId())
                .settlementTime(futuresSettlement.getSettlementTime())
                .avgPrice(futuresSettlement.getAveragePrice())
                .position("")
                .settlePrice(futuresSettlement.getSettlementPrice())
                .build();
    }

    public static FuturesAccountResult toFuturesAccountResult(FuturesCoinAsset futuresCoinAsset) {
        return FuturesAccountResult.builder()
                .tokenId(futuresCoinAsset.getTokenId())
                .availableMargin(futuresCoinAsset.getAvailableMargin())
                .orderMargin(futuresCoinAsset.getOrderMargin())
                .positionMargin(futuresCoinAsset.getPositionMargin())
                .total(futuresCoinAsset.getTotal())
                .build();
    }

    private static List<RiskLimit> toRiskLimits(List<FuturesRiskLimit> riskLimits) {
        return riskLimits.stream()
                .map(FuturesTools::toRiskLimit)
                .collect(Collectors.toList());
    }

    private static RiskLimit toRiskLimit(FuturesRiskLimit t) {
        return RiskLimit.builder()
                .riskLimitId(String.valueOf(t.getRiskLimitId()))
                .quantity(t.getRiskLimitAmount())
                .maintMargin(t.getMaintainMargin())
                .initialMargin(t.getInitialMargin())
                .build();
    }

    public static FundingRateResult toFundingRateResult(FundingRate fundingRate) {
        return FundingRateResult.builder()
                .symbol(fundingRate.getTokenId())
                .intervalEnd(fundingRate.getNextSettleTime())
                // 目前是八小时结算一次
                .intervalStart(fundingRate.getNextSettleTime() - MILLSECONDS_WITH_8_HOUR)
                .rate(fundingRate.getFundingRate()) // 使用预测资金费率
                .build();
    }

    public static InsuranceFundResult toInsuranceFundResult(InsuranceFund insuranceFund) {
        return InsuranceFundResult.builder()
                .id(insuranceFund.getId())
                .timestamp(insuranceFund.getDt())
                .value(insuranceFund.getAvailable())
                .unit(insuranceFund.getTokenId())
                .build();
    }

    public static Tradeable toTradeable(FuturesTradeAble tradeAble) {
        return Tradeable.builder()
                .avaiable(tradeAble.getProfitLoss().getCoinAvailable())
                .margin(tradeAble.getProfitLoss().getMargin())
                .realizedPnL(tradeAble.getProfitLoss().getRealisedPnl())
                .unrealizedPnL(tradeAble.getProfitLoss().getUnrealisedPnl())
                .build();
    }

    /**
     * 将openapi的多仓空仓标识转换成Broker端的标识
     * LONG -> 1
     * SHORT -> 0
     *
     * @param positionSide 多仓空仓标识
     * @return Broker端多仓空仓标识
     */
    public static String toBrokerPositionLongString(String positionSide) {
        return ApiFuturesPositionSide.valueOf(positionSide).equals(ApiFuturesPositionSide.LONG) ? "1" : "0";
    }

    public static boolean toBrokerPositionLongBool(String positionSide) {
        return toBrokerPositionLongString(positionSide).equals("1");
    }

    public static FuturesOrderSide getFuturesSide(OrderSide orderSide, boolean isClose) {
        if (isClose) {
            return orderSide == OrderSide.BUY ? FuturesOrderSide.BUY_CLOSE : FuturesOrderSide.SELL_CLOSE;
        }
        return orderSide == OrderSide.BUY ? FuturesOrderSide.BUY_OPEN : FuturesOrderSide.SELL_OPEN;
    }

    public static OrderSide getOrderSide(FuturesOrderSide futuresOrderSide) {
        if (futuresOrderSide == FuturesOrderSide.BUY_CLOSE || futuresOrderSide == FuturesOrderSide.BUY_OPEN) {
            return OrderSide.BUY;
        } else {
            return OrderSide.SELL;
        }
    }

    public static PlanOrder.FuturesOrderType toFuturesOrderType(String orderType) {
        ApiFuturesOrderType apiFuturesOrderType = ApiFuturesOrderType.valueOf(orderType);
        switch (apiFuturesOrderType) {
            case LIMIT:
            case LIMIT_MAKER:
            case LIMIT_FREE:
                return PlanOrder.FuturesOrderType.LIMIT;
            case STOP:
                return PlanOrder.FuturesOrderType.STOP;
            default:
                throw new OpenApiException(ApiErrorCode.BAD_REQUEST);
        }
    }

    public static FuturesPriceType toFuturesPriceType(String priceType) {
        ApiFuturesPriceType apiFuturesPriceType = ApiFuturesPriceType.valueOf(priceType);
        switch (apiFuturesPriceType) {
            case OVER:
                return FuturesPriceType.OVER;
            case INPUT:
                return FuturesPriceType.INPUT;
            case QUEUE:
                return FuturesPriceType.QUEUE;
            case MARKET:
                return FuturesPriceType.MARKET_PRICE;
            case OPPONENT:
                return FuturesPriceType.OPPONENT;
            default:
                throw new OpenApiException(ApiErrorCode.BAD_REQUEST);
        }
    }

    public static ApiFuturesPriceType toApiFuturesPriceType(FuturesPriceType futuresPriceType) {
        if (futuresPriceType == FuturesPriceType.MARKET_PRICE) {
            return ApiFuturesPriceType.MARKET;
        }

        return ApiFuturesPriceType.valueOf(futuresPriceType.name());
    }

    public static SocketFuturesPositionInfo toSocketFuturesPositionInfo(io.bhex.base.account.FuturesPosition bhPosition, SymbolResult symbol) {
        // 基本数量精度
        DecimalFormatter baseFmt = new DecimalFormatter(new BigDecimal(symbol.getBasePrecision()));
        // 保证金精度
        DecimalFormatter marginFmt = new DecimalFormatter(new BigDecimal(symbol.getTokenFutures().getMarginPrecision()));
        // 价格精度
        DecimalFormatter priceFmt = new DecimalFormatter(new BigDecimal(symbol.getMinPricePrecision()));

        BigDecimal contractMultiplier = new BigDecimal(symbol.getTokenFutures().getContractMultiplier());
        BigDecimal avgPrice = bigDecimalDiv(DecimalUtil.toBigDecimal(bhPosition.getOpenValue()),
                DecimalUtil.toBigDecimal(bhPosition.getTotal()).multiply(contractMultiplier));


        String avgPriceVal = symbol.getIsReverse() ? priceFmt.reciprocalFormat(avgPrice) : priceFmt.format(avgPrice);
        String liquidationPriceVal = symbol.getIsReverse() ? priceFmt.reciprocalFormat(bhPosition.getLiquidationPrice())
                : priceFmt.format(bhPosition.getLiquidationPrice());

        ApiFuturesPositionSide positionSide;
        if (symbol.getIsReverse()) {
            positionSide = bhPosition.getIsLong() ? ApiFuturesPositionSide.SHORT : ApiFuturesPositionSide.LONG;
        } else {
            positionSide = bhPosition.getIsLong() ? ApiFuturesPositionSide.LONG : ApiFuturesPositionSide.SHORT;
        }

        return SocketFuturesPositionInfo.builder()
                .eventTime(System.currentTimeMillis())
                .accountId(String.valueOf(bhPosition.getAccountId()))
                .symbol(bhPosition.getTokenId())
                .side(positionSide)
                .avgPrice(avgPriceVal)
                .position(baseFmt.format(bhPosition.getTotal()))
                .available(baseFmt.format(bhPosition.getAvailable()))
                .flp(liquidationPriceVal)
                .margin(marginFmt.format(bhPosition.getMargin()))
                .realizedPnL(marginFmt.format(bhPosition.getRealisedPnl()))
                .build();
    }

    public static BigDecimal bigDecimalDiv(BigDecimal a, BigDecimal b) {
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return a.divide(b, 18, RoundingMode.DOWN);
    }
}
