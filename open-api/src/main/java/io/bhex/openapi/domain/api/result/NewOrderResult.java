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
public class NewOrderResult {

    private Long accountId;

    private String symbol;

    private String symbolName;

    private String clientOrderId;

    private Long orderId;

    private Long transactTime;

    private String price;

    private String origQty;

    private String executedQty;

    private String status;

    private String timeInForce;

    private String type;

    private String side;

    public static NewOrderResult convert(Order result) {
        return NewOrderResult.builder()
                .symbol(result.getSymbolId())
                .clientOrderId(result.getClientOrderId())
                .orderId(result.getOrderId())
                .transactTime(result.getTime())
                .build();
    }

    public static NewOrderResult convertToResult(Order result) {
        return NewOrderResult.builder()
                .accountId(result.getAccountId())
                .symbol(result.getSymbolId())
                .symbolName(result.getSymbolName())
                .clientOrderId(result.getClientOrderId())
                .orderId(result.getOrderId())
                .transactTime(result.getTime())
                .price(result.getPrice())
                .origQty(result.getOrigQty())
                .executedQty(result.getExecutedQty())
                .status(result.getStatusCode())
                .timeInForce(result.getTimeInForce().name())
                .type(result.getOrderType().name())
                .side(result.getOrderSide().name())
                .build();
    }

}
