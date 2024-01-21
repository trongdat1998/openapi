/*
 ************************************
 * @项目名称: broker-parent
 * @文件名称: ParamsFormatter
 * @Date 2018/09/11
 * @Author will.zhao@bhex.io
 * @Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 **************************************
 */
package io.bhex.openapi.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ParamsFormatter {

    private static final Set<String> LOG_SKIP_PARAMS = new HashSet<>(
            Arrays.asList("token", "token", "sign", "key", "password"));

    private static final String PARAM_SEPARATOR = "\\\\";

    public String formatParams(HttpServletRequest request, HttpServletResponse response) {
        return shouldLogParams(request, response) ? formatParams(request.getParameterMap()) : "";
    }

    private boolean shouldLogParams(HttpServletRequest request, HttpServletResponse response) {
        return "GET".equals(request.getMethod()) || response.getStatus() >= 400;
    }

    private String formatParams(Map<String, String[]> paramMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> e : paramMap.entrySet()) {
            if (e.getValue().length > 0) {
                if (LOG_SKIP_PARAMS.contains(e.getKey())) {
                    for (String value : e.getValue()) {
                        sb.append(e.getKey()).append('=');
                        sb.append("?x").append(value.length()).append(PARAM_SEPARATOR);
                    }
                } else {
                    for (String value : e.getValue()) {
                        sb.append(e.getKey()).append('=');
                        sb.append(value).append(PARAM_SEPARATOR);
                    }
                }
            }
        }
        if (sb.length() - PARAM_SEPARATOR.length() >= 0) {
            sb.setLength(sb.length() - PARAM_SEPARATOR.length());
        }
        return sb.toString();
    }
}
