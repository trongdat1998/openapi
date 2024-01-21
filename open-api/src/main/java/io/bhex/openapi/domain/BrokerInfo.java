/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/6/28
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Getter;

@Builder(builderClassName = "Builder", toBuilder = true)
@Getter
public class BrokerInfo {

    private final Long id;
    private final Long orgId;
    private final String brokerName;
    private final String brokerDomain;
    private final String privateKey;
    private final String publicKey;
    private final Integer status;
    private final Long created;

}
