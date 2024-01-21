package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class WithdrawDetailResult {

    private Long time; //时间
    private Long orderId;
    private Long accountId; //账户Id
    private String token; //token代码
    private String tokenId; //token代码
    private String tokenName; //token代码
    private String address; //提币地址
    private String addressExt; //提币地址
    private String quantity; //用户输入的提币数量
    private String arriveQuantity; // 到账数量
    private Integer status;
    private String statusCode; //状态标识
    private String txid;
    private String txidUrl;
    private Long walletHandleTime;
    private String addressUrl;
    private Integer requiredConfirmNum;
    private Integer confirmNum;
    private String kernelId;
    private Boolean isInternalTransfer;
    private String feeTokenId;
    private String feeTokenName;
    private String fee;

}
