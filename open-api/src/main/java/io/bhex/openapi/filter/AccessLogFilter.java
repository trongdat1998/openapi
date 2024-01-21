/*
 ************************************
 * @项目名称: broker-parent
 * @文件名称: AccessLogFilter
 * @Date 2018/09/11
 * @Author will.zhao@bhex.io
 * @Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 **************************************
 */
package io.bhex.openapi.filter;

import io.bhex.broker.common.util.RequestUtil;
import io.bhex.openapi.util.ParamsFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Deprecated
public class AccessLogFilter implements Filter {

    private Logger accessLogger = LoggerFactory.getLogger("http_access");

    private static ParamsFormatter paramsFormatter;

    static {
        paramsFormatter = new ParamsFormatter();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse rep = (HttpServletResponse) response;

        long start = System.currentTimeMillis();
        chain.doFilter(request, response);
        long cost = System.currentTimeMillis() - start;

        try {
            accessLogger.info(accessLog(req, rep, cost));
        } catch (Exception e) {
            log.error("doFilter error", e);
        }
    }

    private String accessLog(HttpServletRequest req, HttpServletResponse rep, long cost) {
        Object[] items = new Object[]{
                rep.getStatus(),
                req.getMethod(),
                req.getHeader("HOST"),
                req.getServletPath(),
                paramsFormatter.formatParams(req, rep),
                RequestUtil.getRemoteIp(req),
                cost
        };
        return StringUtils.join(items, "|");
    }

    @Override
    public void destroy() {
    }

}
