package io.bhex.openapi.controller;

import com.google.gson.JsonObject;

import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.openapi.constant.ApiErrorCode;
import io.bhex.openapi.domain.AccountType;
import io.bhex.openapi.domain.api.enums.ApiFuturesPositionSide;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.interceptor.OpenApiInterceptor;
import io.bhex.openapi.interceptor.annotation.LimitAuth;
import io.bhex.openapi.interceptor.annotation.SignAuth;
import io.bhex.openapi.util.RequestUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;

import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.service.BrokerService;
import io.bhex.openapi.util.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping(value = "/", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class GeneralApiController {

    @Autowired
    BasicService basicService;

    @Autowired
    BrokerService brokerService;

    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping(value = {"/openapi/brokerInfo", "/openapi/v1/brokerInfo"})
    public String getBrokerInfo(Header header,
                                @RequestParam(name = "type", required = false) String tradeType) {
        return ResultUtils.toRestJSONString(brokerService.getBrokerInfo(header, tradeType));
    }

    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping({"/openapi/v1/time", "/openapi/time", "/time"})
    @ResponseBody
    public String serverTime(Header header, @RequestParam(required = false, defaultValue = "0") Long sleeps) {
        JsonObject dataObj = new JsonObject();
        dataObj.addProperty("serverTime", System.currentTimeMillis());
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    @LimitAuth(limitTypes = {RateLimitType.REQUEST_WEIGHT}, weight = 1)
    @RequestMapping({"/openapi/v1/ping", "/openapi/ping", "/ping"})
    @ResponseBody
    public String ping() {
        JsonObject dataObj = new JsonObject();
        return JsonUtil.defaultGson().toJson(dataObj);
    }

    @RequestMapping(value = {"/openapi/ip", "/openapi/real_ip"})
    @ResponseBody
    public String realIp(HttpServletRequest request) {
        return JsonUtil.defaultGson().toJson(RequestUtil.getRemoteIp(request));
    }

    @RequestMapping(value = {"/openapi/getOptions", "/openapi/v1/getOptions"})
    public String getOptions(Header header, @RequestParam(required = false, defaultValue = "0") Boolean expired) {
        return ResultUtils.toRestJSONString(brokerService.getOptionTokens(header, expired));
    }

    @RequestMapping(value = {"/openapi/getContracts", "/openapi/v1/getContracts"})
    public String getContracts(Header header, @RequestParam(required = false, defaultValue = "0") Boolean expired) {
        return ResultUtils.toRestJSONString(brokerService.getFuturesTokens(header, expired));
    }

    @RequestMapping(value = {"/openapi/pairs", "/openapi/v1/pairs"})
    public String getPairs(Header header, @RequestParam(required = false, defaultValue = "0") Boolean expired) {
        return ResultUtils.toRestJSONString(brokerService.getPairs(header, expired));
    }

    @RequestMapping(value = {"/openapi/contracts", "/openapi/v1/contracts"})
    public String contracts(Header header, @RequestParam(required = false, defaultValue = "0") Boolean expired) {
        return ResultUtils.toRestJSONString(brokerService.getContracts(header, expired));
    }

    @RequestMapping(value = {"/openapi/send_time_flux"})
    public Flux<String> sendServerTimeFlux(@RequestParam(required = false, defaultValue = "5") Integer interval) {
        return Flux.interval(Duration.ofSeconds(interval))
                .map(seq -> LocalTime.now().toString());
    }

    @RequestMapping(value = {"/openapi/send_time_sse"})
    public Flux<ServerSentEvent<String>> sendServerTimeSSE(@RequestParam(required = false, defaultValue = "5") Integer interval) {
        AtomicInteger index = new AtomicInteger();
        return Flux.interval(Duration.ofSeconds(interval))
                .map(seq -> Tuples.of(index.getAndIncrement(), LocalTime.now().toString()))
                .map(data -> ServerSentEvent.<String>builder()
                        .event("server_time")
                        .id(String.valueOf(data.getT1()))
                        .data(data.getT2())
                        .build());
    }

}
