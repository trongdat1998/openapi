/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain.websocket
 *@Date 2018/7/12
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain.websocket;

import com.google.gson.JsonElement;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class PushMessage {

    private Integer code = 200;
    private String id;
    private String topic;
    private String event;
    private WebSocketParam.ExtData extData;
    private JsonElement data;

}
