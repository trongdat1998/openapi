/**********************************
 *@项目名称: api-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/10/17
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class BalanceFlowResult {

    private Long id;
    private Long accountId;
    private String token;
    private String tokenId;
    private String tokenName;
    private Integer flowTypeValue;
    private String flowType;
    private String flowName;
    private String change;
    private String total;
    private Long created;

}
