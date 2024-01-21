/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain.websocket
 *@Date 2018/7/10
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketParam {

    private String id;
    private String topic;
    private String event;
    private ExtData extData;

    @Data
    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtData {
        private String dataType;
        private Long accountId;
        private String symbolId;
        private Long fromId = 0L;
        private Integer limit = 20;
    }

}
