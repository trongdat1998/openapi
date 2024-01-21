/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/6/26
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

import com.google.common.base.Strings;
import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;

public enum OrderType {
    MARKET("market"),
    LIMIT("limit");

    private String value;

    OrderType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public static OrderType fromValue(String value) {
        String side = Strings.nullToEmpty(value).toUpperCase();
        switch (side) {
            case "MARKET":
                return OrderType.MARKET;
            case "LIMIT":
                return OrderType.LIMIT;
            default:
                throw new BrokerException(BrokerErrorCode.ORDER_TYPE_ILLEGAL);
        }
    }

}
