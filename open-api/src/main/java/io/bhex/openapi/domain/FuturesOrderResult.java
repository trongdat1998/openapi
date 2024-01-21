package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.core.annotation.AliasFor;

import java.util.List;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class FuturesOrderResult {

    /**
     * 下单时间
     */
    private Long time;

    /**
     * 订单最后更新时间
     */
    private Long updateTime;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 客户端订单ID
     */
    private String clientOrderId;

    /**
     * 币对ID
     */
    private String symbol;

    /**
     * 下单价格
     */
    private String price;

    /**
     * 杠杆
     */
    private String leverage;

    /**
     * 原始下单数量
     */
    private String origQty;

    /**
     * 成交量
     */
    private String executedQty;

    private String executeQty;

    /**
     * 成交金额
     */
    private String executedAmount;

    /**
     * 成交均价
     */
    private String avgPrice; // 成交均价

    /**
     * 保证金
     */
    private String marginLocked;

    /**
     * 订单类型
     * 可选值：LIMIT, LIMIT_MAKER, STOP
     */
    private String orderType; //订单类型

    /**
     * 买卖方向
     * 可选值：BUY_OPEN, SELL_OPEN, BUY_CLOSE, SELL_CLOSE
     */
    private String side;

    /**
     * 手续费
     */
    private @Singular("fee") List<OrderMatchFeeInfo> fees;

    /**
     * Time in force
     * 可选值：GTC, FOK, IOC
     */
    private String timeInForce;

    /**
     * 订单状态标识
     */
    private String status;

    /**
     * 是否是平仓单
     */
    private Boolean isClose;

    /**
     * 价格类型
     * 可选值：INPUT, OPPONENT, QUEUE, OVER, MARKET
     */
    private String priceType;

    /**
     * 计划委托触发价格
     */
    private String triggerPrice;

    /**
     * 是否为系统强平单
     */
    private Boolean isLiquidationOrder;
}
