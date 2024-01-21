package io.bhex.openapi.domain.api.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CancelOrderResult {

    private Long accountId;

    private Long exchangeId;

    private String symbol;

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

}
