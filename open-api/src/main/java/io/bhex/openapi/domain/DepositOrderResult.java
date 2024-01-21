/**********************************
 *@项目名称: openapi
 *@文件名称: io.bhex.openapi.domain
 *@Date 2018/12/10
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class DepositOrderResult {

    private Long time; //时间
    private Long orderId;
    private String token; //token代码
    private String tokenName;
    private String address; //用户充币地址
    private String addressTag; //用户充币地址
    private String fromAddress; // 用户充币使用的
    private String fromAddressTag; // 用户充币使用的
    private String quantity; //充币到账数量

    private Integer status;
    private String statusCode; //状态标识
    private Integer requiredConfirmNum;
    private Integer confirmNum;
    private String txid;
    private String txidUrl;
}
