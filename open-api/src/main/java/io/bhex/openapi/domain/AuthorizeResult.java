/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/8/1
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class AuthorizeResult {

    private Boolean bindMobile;
    private Boolean bindEmail;
    private Boolean bindGA;
    private String requestId;
    private String token;
    private Long userId;
    private Long defaultAccountId;
    private Integer registerType;
    private String mobile;
    private String email;

}
