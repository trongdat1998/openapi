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
public class DepositAddressResult {

    private Boolean allowDeposit;
    private String address;
    private String addressExt;
    private String minQuantity; // 最小充币量D
    private Boolean needAddressTag;
    private Integer requiredConfirmNum;
    private Integer canWithdrawConfirmNum;
    private String tokenType;

}
