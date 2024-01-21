/**********************************
 *@项目名称: openapi
 *@文件名称: io.bhex.openapi.domain
 *@Date 2020-10-09
 *@Author bingqiang.yuan
 *@Copyright（C）: 2020 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class TransferTokenResult {
    private String clientOrderId;
    private Boolean result;
    private String refuseReason;
}
