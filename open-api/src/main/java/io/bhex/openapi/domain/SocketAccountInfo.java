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

import java.util.List;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class SocketAccountInfo {

    @SerializedName("e")
    private String eventType;

    @SerializedName("E")
    private Long eventTime;

    @SerializedName("m")
    private String makerCommission;

    @SerializedName("t")
    private String takerCommission;

    @SerializedName("b")
    private String buyerCommission;

    @SerializedName("s")
    private String sellerCommission;

    @SerializedName("T")
    private Boolean canTrade;

    @SerializedName("W")
    private Boolean canWithdraw;

    @SerializedName("D")
    private Boolean canDeposit;

    @SerializedName("u")
    private Long lastUpdated;

    @SerializedName("B")
    private List<SocketBalanceInfo> balanceChangedList;

}
