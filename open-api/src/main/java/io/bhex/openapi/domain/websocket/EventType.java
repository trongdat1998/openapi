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
public enum EventType {

    SUBSCRIBE("sub"),
    CANCEL("cancel"),
    CANCEL_ALL("cancel_all"),
    ASK_DATA("req");

    private String value;

    EventType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public static EventType fromValue(String event) {
        if (Strings.isNullOrEmpty(event)) {
            return null;
        }
        for (EventType eventType : EventType.values()) {
            if (eventType.value.equalsIgnoreCase(event)) {
                return eventType;
            }
        }
        return null;
    }

}
