/**********************************
 *@项目名称: openapi
 *@文件名称: io.bhex.broker
 *@Date 2018/9/29
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi;

import io.bhex.base.grpc.client.channel.IGrpcClientPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;

import javax.annotation.Resource;

@Slf4j
public class BrokerInitializer {

    @Resource
    private IGrpcClientPool pool;

    @EventListener(value = {ContextStoppedEvent.class})
    public void contextStoppedAlert() {
        log.warn("[ALERT] context is stopped, please check!!!");
        pool.shutdown();
    }

    @EventListener(value = {ContextClosedEvent.class})
    public void contextClosedAlert() {
        log.warn("[ALERT] context is closed, please check!!!");
        pool.shutdown();
    }

}
