/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/6/25
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class QuoteResult {

    private long time; //时间
    private String close; //最新价
    private String high; //最高价
    private String low; //最低价
    private String open; //开盘价
    private String volume; //成交量
//    private String cnyValue;
//    private String usdValue;
//    private String currencyValue;

}
