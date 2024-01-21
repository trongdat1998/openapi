/**********************************
 *@项目名称: openapi
 *@文件名称: io.bhex.openapi.util
 *@Date 2018/12/10
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.util;

import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.constant.OpenApiException;
import io.bhex.openapi.domain.BrokerConstants;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

/**
 * 只读ApiKey校验
 */
@Slf4j
public class ReadOnlyApiKeyCheckUtil {

    public static void checkApiKeyReadOnly(HttpServletRequest request) {
        Integer apiKeyType = (Integer) request.getAttribute(BrokerConstants.API_KEY_TYPE);
        if (apiKeyType == BrokerConstants.API_KEY_READ_ONLY_TYPE) {
            log.warn("read only api_key has a unsupported operation");
            throw new OpenApiException(ApiErrorCode.UNSUPPORTED_OPERATION);
        }
    }

}
