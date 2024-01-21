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
public class BestOrderInfo {

    private Long time; //时间
    private Long orderId; // 订单ID
    private Long accountId; //账户ID
    private String price; //下单价格
    private String origQty; //原始下单数量
    private String type; //订单类型
    private String side; //买卖方向
    private String status; //状态标识

}
