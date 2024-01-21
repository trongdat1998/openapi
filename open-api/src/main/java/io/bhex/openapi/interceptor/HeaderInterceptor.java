/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.interceptor
 *@Date 2018/6/21
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.interceptor;

import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.entity.RequestPlatform;
import io.bhex.broker.common.util.HeaderUtil;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.BrokerInfo;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.util.ApplicationContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Deprecated
public class HeaderInterceptor implements HandlerInterceptor {

    public static final String HEADER_REQUEST_ATTR = HeaderInterceptor.class + ".HEADER_REQUEST_ATTR";

    private BasicService basicService;

    public HeaderInterceptor() {
        this.basicService = ApplicationContextUtil.getBean("basicService");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Header header = HeaderUtil.buildFromHttp(request);
        String serverName = request.getServerName();
        BrokerInfo brokerInfo = basicService.getByRequestHost(serverName);
        if (brokerInfo == null) {
            throw new OpenApiException(ApiErrorCode.DISCONNECTED);
        }
        // 构建header 包括brokerId等信息
        Header.Builder builder = header.toBuilder();
        builder.orgId(brokerInfo.getOrgId());
        builder.platform(RequestPlatform.OPENAPI);
        builder.domain(brokerInfo.getBrokerDomain());
        request.setAttribute(HeaderInterceptor.HEADER_REQUEST_ATTR, builder.build());
        return true;
    }
}
