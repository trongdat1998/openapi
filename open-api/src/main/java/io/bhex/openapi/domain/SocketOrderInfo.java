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
@Builder(builderClassName = "Builder")
public class SocketOrderInfo {

    @SerializedName("e")
    private String eventType;

    @SerializedName("E")
    private Long eventTime;

    @SerializedName("s")
    private String symbol;

    @SerializedName("c")
    private String clientOrderId;

    @SerializedName("S")
    private String orderSide;

    @SerializedName("o")
    private String orderType;

    @SerializedName("f")
    private String timeInForce;

    @SerializedName("q")
    private String quantity;

    @SerializedName("p")
    private String price;

//    private String stopPrice;
//
//    private String icebergQuantity;
//
//    private Integer g;
//
//    private String currentExecutionType;

    @SerializedName("X")
    private String status;
//
//    private String rejectReason;

    @SerializedName("i")
    private Long orderId;

    @SerializedName("M")
    private Long matchOrderId;

    @SerializedName("l")
    private String lastExecutedQuantity;

    @SerializedName("z")
    private String executedQuantity;

    @SerializedName("L")
    private String lastExecutedPrice;

    @SerializedName("n")
    private String commissionAmount;

    @SerializedName("N")
    private String commissionAsset;

    @SerializedName("u")
    private Boolean isNormal;
//
//    private Long transactionTime;
//
//    private Long tradeId;
//
//    private Integer I; // ignore

    @SerializedName("w")
    private Boolean isWorking;

    @SerializedName("m")
    private Boolean isMaker;
//
//    private Boolean M;

    @SerializedName("O")
    private Long createTime;

    @SerializedName("Z")
    private String executedAmount;

    @SerializedName("A")
    private Long matchAccountId;

    @SerializedName("C")
    private Boolean isClose;

    @SerializedName("v")
    private String leverage;

}
