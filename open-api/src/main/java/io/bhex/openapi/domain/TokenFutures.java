package io.bhex.openapi.domain;

import io.bhex.openapi.domain.futures.RiskLimit;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class TokenFutures {

    private Long id;

    /**
     * 不用数字,期货名称
     */
    private String tokenId;

    /**
     * 前端显示计价的token_id
     */
    private String displayTokenId;

    /**
     * 生效/上线日期
     */
    private Long issueDate;

    /**
     * 到期/交割时间
     */
    private Long settlementDate;

    /**
     * 交易(quote)token
     */
    private String coinToken;

    /**
     * 计价单位(token_id)
     */
    private String currency;

    /**
     * 显示价格单位
     */
    private String currencyDisplay;

    /**
     * 合约乘数
     */
    private String contractMultiplier;

    /**
     * 交易时段内下跌限价
     */
    private String limitDownInTradingHours;

    /**
     * 交易时段内上涨限价
     */
    private String limitUpInTradingHours;

    /**
     * 交易时段外下跌限价
     */
    private String limitDownOutTradingHours;

    /**
     * 交易时段外上涨限价
     */
    private String limitUpOutTradingHours;

    /**
     * 最大杠杆
     */
    private String maxLeverage;
    /**
     * 杠杆列表
     */
    private List<String> levers;
    /**
     * 风险限额列表
     */
    private List<RiskLimit> riskLimits;

    /**
     * 杠杆范围
     */
    private String leverageRange;
    /**
     * 超价浮动范围
     */
    private List<String> overPriceRange;
    /**
     * 市价浮动范围
     */
    private List<String> marketPriceRange;
    /**
     * 标的指数
     */
    private String indexToken;

    /**
     * 前端显示标的指数（如果是正向，和indexToken一样）
     */
    private String displayIndexToken;

    /**
     * 保证金精度
     */
    private String marginPrecision;

    /**
     * 前端显示的标的
     */
    private String displayUnderlyingId;
}
