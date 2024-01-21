/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/8/5
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class ApiKeyResult {

    private Long id;
//    private Long userId;
//    private Long accountId;
//    private Integer accountType;
//    private Integer accountIndex;
//    private String accountName;
    private String apiKey;
    private String securityKey;
    private String tag;
    private Integer type;
    private Integer level;
    private String ipWhiteList;
    private Integer status;
    private Long created;
    private Long updated;

}
