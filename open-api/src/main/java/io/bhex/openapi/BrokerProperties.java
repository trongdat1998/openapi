/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker
 *@Date 2018/6/10
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi;

import io.bhex.broker.common.entity.GrpcClientProperties;
import io.bhex.broker.common.objectstorage.AwsObjectStorageProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "broker")
@Data
public class BrokerProperties {

    private String grpcKeyPath = "";
    private String grpcCrtPath = "";
    private GrpcClientProperties grpcClient = new GrpcClientProperties();

}
