/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/6/26
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class MatchResult {

    private Long time; // 成交时间（撮合时间）
    private Long tradeId; // 成交ID
    private Long orderId; // 订单ID
    private Long matchOrderId; // 对手方订单ID
    private Long accountId; // 账户ID
    private String symbolId; //币对
    private String symbolName; //币对
    private String baseTokenId;
    private String baseTokenName;
    private String quoteTokenId;
    private String quoteTokenName;
    private String price; // 成交价
    private String quantity; // 成交量
    private String feeTokenId; // 手续费token
    private String feeTokenName; // 手续费token
    private String fee; // 手续费
    private String makerRebate;
    private String type; //订单类型
    private String side; //买卖方向

    private String symbol;
    private String timeInForce;
}
