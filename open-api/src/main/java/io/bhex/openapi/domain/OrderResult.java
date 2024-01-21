/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/6/26
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class OrderResult {

    private Long time; //时间
    private Long updateTime;
    private Long orderId; // 订单ID
    private Long accountId; //账户ID
    private String clientOrderId;
    private String symbol; //币对Id
    private String price; //下单价格
    private String origQty; //原始下单数量
    private String executedQty; //成交量
    private String executedAmount; //成交量
    private String avgPrice; // 成交均价
    private String type; //订单类型
    private String side; //买卖方向
    private String status; //状态标识


    //
    private String option;
    private String timeInForce;
    private @Singular("fee")
    List<OrderMatchFeeInfo> fees;
}
