/**********************************
 *@项目名称: broker-parent
 *@文件名称:
 *@Date 2018/6/25
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class OptionTokenResult {

    private Long orgId;
    private transient Long exchangeId;
    private String token;
    private String tokenId;
    private String tokenName;
    private String tokenFullName;
    private String iconUrl;
    private String maxWithdrawQuota;
    private String minWithdrawQuantity;
    private String minDepositQuantity;
    private String feeToken;
    private String feeTokenId;
    private String feeTokenName;
    private String fee;
    private Boolean allowWithdraw; // 是否允许提现
    private Boolean allowDeposit; // 是否允许充值
    private List<SymbolResult> baseTokenSymbols;
    private List<SymbolResult> quoteTokenSymbols;
    private Boolean isEOS;
    private String tokenType;
    private Boolean needAddressTag;
    private String needKycQuantity;
    private TokenOptionResult baseTokenOption;//期权基础信息

}
