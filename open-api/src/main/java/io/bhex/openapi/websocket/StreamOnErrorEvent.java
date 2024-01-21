/**********************************
 *@项目名称: openapi
 *@文件名称: io.bhex.broker.websocket
 *@Date 2018/9/3
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.websocket;

import org.springframework.context.ApplicationEvent;

public class StreamOnErrorEvent extends ApplicationEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public StreamOnErrorEvent(Object source) {
        super(source);
    }
}
