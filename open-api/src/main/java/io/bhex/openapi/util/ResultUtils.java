package io.bhex.openapi.util;

import io.bhex.broker.common.util.JsonUtil;

public class ResultUtils {

    public static String toRestJSONString(Object object) {
        return JsonUtil.defaultGson().toJson(object);
    }

}
