package io.bhex.openapi.domain.websocket;

import com.google.common.base.Strings;

/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain.websocket
 *@Date 2018/7/10
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
public enum TopicType {

    MATCH("match"),
    ORDER("order"),
    ORDER_FILLED("order_filled"),
    BALANCE("balance"),
    FUTURES_POSITION("futures_position"),
    TICKET_INFO("ticket_info"),
    SYSTEM("system");

    private String value;

    TopicType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public static TopicType fromValue(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }
        for (TopicType topicType : TopicType.values()) {
            if (topicType.value.equalsIgnoreCase(value)) {
                return topicType;
            }
        }
        return null;
    }

}
