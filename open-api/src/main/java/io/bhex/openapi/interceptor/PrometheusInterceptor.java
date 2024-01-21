/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.interceptor
 *@Date 2018/7/15
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.interceptor;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Deprecated
public class PrometheusInterceptor implements HandlerInterceptor {

    private static final Counter COUNTER = Counter.build()
            .namespace("broker")
            .subsystem("controller")
            .name("request_total")
            .labelNames("bean", "method")
            .help("Total number of http request started")
            .register();

    private static final Summary SUMMARY = Summary.build()
            .namespace("broker")
            .subsystem("controller")
            .name("summary_latency_seconds")
            .labelNames("bean", "method", "status", "exception")
            .help("Summary of controller handle latency in milliseconds")
            .register();

    private static final Histogram HISTOGRAM = Histogram.build()
            .namespace("broker")
            .subsystem("controller")
            .name("histogram_latency_seconds")
            .labelNames("bean", "method")
            .help("Histogram of controller handle latency in milliseconds")
            .register();

    private static final String HOLDER_REQUEST_ATTR = PrometheusInterceptor.class.getName() + ".HOLDER_REQUEST_ATTR";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getAttribute(HOLDER_REQUEST_ATTR) != null) {
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Holder holder = new Holder(handlerMethod.getBeanType().getName(), handlerMethod.getMethod().getName(), System.currentTimeMillis());
        request.setAttribute(HOLDER_REQUEST_ATTR, holder);
        COUNTER.labels(holder.bean, holder.method).inc();
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Holder holder = (Holder) request.getAttribute(HOLDER_REQUEST_ATTR);
        if (holder != null) {
            Double latencyMillieconds = (System.currentTimeMillis() - holder.beginTime) / 1.0D;

            String[] labelValues = new String[]{holder.bean, holder.method, String.valueOf(response.getStatus()), ex == null ? "none" : ex.getClass().getName()};
            SUMMARY.labels(labelValues).observe(latencyMillieconds);

            HISTOGRAM.labels(holder.bean, holder.method).observe(latencyMillieconds);
        }
    }

    private static class Holder {
        final String bean;
        final String method;
        final long beginTime;

        Holder(String bean, String method, long beginTime) {
            this.bean = bean;
            this.method = method;
            this.beginTime = beginTime;
        }
    }
}
