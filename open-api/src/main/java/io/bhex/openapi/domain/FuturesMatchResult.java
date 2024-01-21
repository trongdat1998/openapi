package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class FuturesMatchResult {

    private Long time; // 成交时间（撮合时间）
    private Long tradeId; // 成交ID
    private Long orderId; // 订单ID
    private Long matchOrderId; // 对手方订单ID
    private String symbolId; //币对
    private String price; // 成交价
    private String quantity; // 成交量
    private String feeTokenId; // 手续费token
    private String fee; // 手续费
    private String makerRebate;
    private String orderType; //订单类型
    private String side; //买卖方向
    private String pnl; //盈亏

}
