package io.bhex.openapi.domain.api.result;

import io.bhex.broker.grpc.order.MatchInfo;
import io.bhex.broker.grpc.order.OrderSide;
import io.bhex.openapi.domain.OrderMatchFeeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeResult {

    private Long id;

    private String symbol;

    private String symbolName;

    private Long orderId;

    private Long matchOrderId;

    private String price;

    private String qty;

    private String commission;

    private String commissionAsset;

    private Long time;

    private Boolean isBuyer;

    private Boolean isMaker;

    private Boolean isNormal;

    private OrderMatchFeeInfo fee;

    private String feeTokenId;

    private String feeAmount;

    private String makerRebate;

    public static TradeResult convert(MatchInfo info) {
        BigDecimal fee = new BigDecimal(info.getFee().getFee());
        BigDecimal makerRebate = BigDecimal.ZERO;
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            makerRebate = fee.abs();
            fee = BigDecimal.ZERO;
        }
        return TradeResult.builder()
                .id(info.getTradeId())
                .symbol(info.getSymbolId())
                .symbolName(info.getSymbolName())
                .orderId(info.getOrderId())
                .matchOrderId(info.getMatchOrderId())
                .price(info.getPrice())
                .qty(info.getQuantity())
                .commission(fee.stripTrailingZeros().toPlainString())
                .commissionAsset(info.getFee().getFeeTokenName())
                .time(info.getTime())
                .isBuyer(info.getOrderSide().equals(OrderSide.BUY))
                .isMaker(info.getIsMaker())
                .fee(OrderMatchFeeInfo.builder()
                        .feeTokenId(info.getFee().getFeeTokenId())
                        .feeTokenName(info.getFee().getFeeTokenName())
                        .fee(fee.stripTrailingZeros().toPlainString())
                        .build())
                .feeTokenId(info.getFee().getFeeTokenId())
                .feeAmount(fee.stripTrailingZeros().toPlainString())
                .makerRebate(makerRebate.stripTrailingZeros().toPlainString())
                .build();
    }

}
