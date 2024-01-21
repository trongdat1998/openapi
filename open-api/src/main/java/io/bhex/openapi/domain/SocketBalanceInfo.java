/**********************************
 *@项目名称: openapi
 *@文件名称: io.bhex.openapi.domain
 *@Date 2018/11/7
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class SocketBalanceInfo {

    @SerializedName("a")
    private String asset;

    @SerializedName("f")
    private String free;

    @SerializedName("l")
    private String locked;

}
