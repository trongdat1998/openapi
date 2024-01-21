package io.bhex.openapi.domain.api.result;

import io.bhex.broker.grpc.order.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryOrderResult {

    private Long accountId;

    private Long exchangeId;

    private String symbol;

    private String symbolName;

    private String clientOrderId;

    private Long orderId;

    private String price;

    private String origQty;

    private String executedQty;

    private String cummulativeQuoteQty;

    private String avgPrice;

    private String status;

    private String timeInForce;

    private String type;

    private String side;

    private String stopPrice;

    private String icebergQty;

    private Long time;

    private Long updateTime;

    private Boolean isWorking;

    public static QueryOrderResult convert(Order order) {
        String statusCode = order.getStatusCode();
        // Boolean isWorking = !statusCode.equals(OrderStatus.FILLED) && !statusCode.equals(OrderStatus.CANCELED);
        return QueryOrderResult.builder()
                .accountId(order.getAccountId())
                .exchangeId(order.getExchangeId())
                .symbol(order.getSymbolId())
                .symbolName(order.getSymbolName())
                .clientOrderId(order.getClientOrderId())
                .orderId(order.getOrderId())
                .price(order.getPrice())
                .origQty(order.getOrigQty())
                .executedQty(order.getExecutedQty())
                .cummulativeQuoteQty(order.getExecutedAmount())
                .avgPrice(order.getAvgPrice())
                .status(statusCode)
                .timeInForce(order.getTimeInForce().name())
                .type(order.getOrderType().name())
                .side(order.getOrderSide().name())
                .stopPrice("0.0")
                .icebergQty("0.0")
                .time(order.getTime())
                .updateTime(order.getLastUpdated())
                .isWorking(true)
                .build();
    }

}
