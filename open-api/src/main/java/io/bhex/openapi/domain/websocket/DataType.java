package io.bhex.openapi.domain.websocket;

import com.google.common.base.Strings;

/**********************************
 *@项目名称: openapi
 *@文件名称: io.bhex.broker.domain.websocket
 *@Date 2018/8/30
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
public enum DataType {
    ORDER("order"),
    CURRENT_ORDER("current_order"),
    HISTORY_ORDER("history_order"),
    MATCH("match"),
    HISTORY_MATCH("history_match"),
    BALANCE("balance"),
    CURRENT_BALANCE("current_balance");

    private String value;

    DataType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public static DataType fromValue(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }
        for (DataType dataType : DataType.values()) {
            if (dataType.value.equalsIgnoreCase(value)) {
                return dataType;
            }
        }
        return null;
    }


}
