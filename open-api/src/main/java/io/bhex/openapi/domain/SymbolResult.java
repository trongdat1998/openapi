/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/6/25
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import io.bhex.broker.core.domain.GsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class SymbolResult {

    private Long orgId;
    private Long exchangeId;
    private String exchangeName;
    private String symbolId;
    private String symbolName;
    private String baseTokenId;
    private String baseTokenName;
    private String quoteTokenId;
    private String quoteTokenName;
    private String basePrecision;
    private String quotePrecision;
    private String minTradeQuantity;
    private String minTradeAmount;
    private String minPricePrecision;
    private String digitMerge;
    private QuoteResult quote; // 行情信息
    private Boolean canTrade;
    private Boolean favorite;
    private Integer category;
    private TokenFutures tokenFutures; // 期货Symbol信息
    private Boolean isReverse;
    private Boolean allowMargin;
    @GsonIgnore
    private Boolean hideFromOpenapi;
    @GsonIgnore
    private Boolean forbidOpenapiTrade;
    private Boolean isAggregate;
}
