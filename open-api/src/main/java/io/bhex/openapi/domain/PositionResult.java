package io.bhex.openapi.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * @Author: yuehao  <hao.yue@bhex.com>
 * @CreateDate: 2019/1/8 下午4:16
 * @Copyright（C）: 2018 BHEX Inc. All rights reserved.
 */

@Builder
@AllArgsConstructor
public class PositionResult {

    private Long balanceId; //balanceId

    private String tokenId; //tokenId

    private Long accountId; //accountId

    private String symbol; //tokenId

    private String symbolName; //期权名称

    private String optionType; //多空

    private String position; //持仓量

    private String total; //持仓量

    private String margin; // 持仓保证金

    private Long settlementTime; // 交割时间

    private String strikePrice; //行权价格

    private String price; //现价金额

    private String availablePosition; //可平量

    private String averagePrice; //持仓均价

    private String changedRate; //涨跌幅

    private String changed; //涨跌幅

    private String quoteTokenName;

    private String index; //标的指数

    private String baseTokenId;

    private String baseTokenName;

    private String quoteTokenId;
}