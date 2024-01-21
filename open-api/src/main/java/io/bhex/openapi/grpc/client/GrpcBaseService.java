/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.grpc.client
 *@Date 2018/6/25
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.grpc.client;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.openapi.grpc.config.GrpcClientConfig;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.annotation.Resource;

class GrpcBaseService {

    @Resource
    GrpcClientConfig grpcClientConfig;

    public String printStatusRuntimeException(StatusRuntimeException e) {
        return String.format("code=%s, desc=%s, keys=%s",
                e.getStatus().getCode(),
                e.getStatus().getDescription(),
                e.getTrailers() != null ? e.getTrailers().keys() : "trailers is null");
    }

    public BrokerException commonStatusRuntimeException(StatusRuntimeException exception) {
        if (exception.getStatus() == Status.DEADLINE_EXCEEDED) {
            return new BrokerException(BrokerErrorCode.GRPC_SERVER_TIMEOUT);
        }
        return new BrokerException(BrokerErrorCode.GRPC_SERVER_SYSTEM_ERROR);
    }

}
