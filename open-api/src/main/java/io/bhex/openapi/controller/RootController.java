/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker
 *@Date 2018/7/8
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.entity.RequestPlatform;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.openapi.domain.TransferResult;
import io.bhex.openapi.domain.WithdrawDetailResult;
import io.bhex.openapi.domain.api.result.ThirdPartyUserTokenResult;
import io.bhex.openapi.service.AccountService;
import io.bhex.openapi.service.BalanceService;
import io.bhex.openapi.service.BasicService;
import io.bhex.openapi.util.ResultUtils;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Controller
@Slf4j
public class RootController {

    @Resource
    private BasicService basicService;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate brokerRedisTemplate;

    @Resource
    private BalanceService balanceService;

    @Resource
    private AccountService accountService;


    @RequestMapping(value = "/internal/metrics", produces = TextFormat.CONTENT_TYPE_004)
    @ResponseBody
    public String metrics(@RequestParam(name = "name[]", required = false) String[] names) throws IOException {
        Set<String> includedNameSet = names == null ? Collections.emptySet() : Sets.newHashSet(names);
        Writer writer = new StringWriter();
        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(includedNameSet));
        return writer.toString();
    }

    @ResponseBody
    @RequestMapping(value = "/internal/refresh_cache")
    public String initBasicData() {
        try {
            basicService.initBasicCache();
            return "OK";
        } catch (Exception e) {
            return "Error:" + Throwables.getStackTraceAsString(e);
        }
    }

    @RequestMapping(value = "/internal/redis/set")
    @ResponseBody
    public String internalRedisSetOperation(String key, String value) {
        if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(value)) {
            log.info("redisOperation: set {} {}", key, value);
            brokerRedisTemplate.opsForValue().set(key, value);
            return "OK";
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/redis/hset")
    @ResponseBody
    public String internalRedisHSetOperation(String key, String field, String value) {
        if (Stream.of(key, field, value).noneMatch(Strings::isNullOrEmpty)) {
            log.info("redisOperation: hset {} {} {}", key, field, value);
            brokerRedisTemplate.opsForHash().put(key, field, value);
            return "OK";
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/redis/del")
    @ResponseBody
    public String internalRedisDelOperation(String key) {
        if (!Strings.isNullOrEmpty(key)) {
            log.info("redisOperation: del {}", key);
            brokerRedisTemplate.delete(key);
            return "OK";
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/redis/hdel")
    @ResponseBody
    public String internalRedisHDelOperation(String key, String field) {
        if (Stream.of(key, field).noneMatch(Strings::isNullOrEmpty)) {
            log.info("redisOperation: hdel {} {}", key, field);
            brokerRedisTemplate.opsForHash().delete(key, field);
            return "OK";
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/redis/get")
    @ResponseBody
    public String internalRedisGetOperation(String key) {
        if (!Strings.isNullOrEmpty(key)) {
            log.info("redisOperation: get {}", key);
            String value = brokerRedisTemplate.opsForValue().get(key);
            return Strings.nullToEmpty(value);
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/redis/hget")
    @ResponseBody
    public String internalRedisHGetOperation(String key, String field) {
        if (Stream.of(key, field).noneMatch(Strings::isNullOrEmpty)) {
            log.info("redisOperation: hget {} {}", key, field);
            Object value = brokerRedisTemplate.opsForHash().get(key, field);
            return value == null ? "" : value.toString();
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/redis/hgetall")
    @ResponseBody
    public String internalRedisHGetAllOperation(String key) {
        if (!Strings.isNullOrEmpty(key)) {
            log.info("redisOperation: hgetall {}", key);
            Map<Object, Object> valuesMap = brokerRedisTemplate.opsForHash().entries(key);
            return JsonUtil.defaultGson().toJson(valuesMap);
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/redis/sadd")
    @ResponseBody
    public String internalRedisSAddOperation(String key, String values) {
        if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(values)) {
            log.info("redisOperation: sadd {}", key);
            return "" + brokerRedisTemplate.opsForSet().add(key, values.split(","));
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/redis/smembers")
    @ResponseBody
    public String internalRedisSMembersOperation(String key) {
        if (!Strings.isNullOrEmpty(key)) {
            log.info("redisOperation: members {}", key);
            return JsonUtil.defaultGson().toJson(brokerRedisTemplate.opsForSet().members(key));
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/redis/srem")
    @ResponseBody
    public String internalRedisSRemoveOperation(String key, String value) {
        if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(value)) {
            log.info("redisOperation: members {}", key);
            return "" + brokerRedisTemplate.opsForSet().remove(key, value);
        } else {
            return "error: check param";
        }
    }

    @RequestMapping(value = "/internal/transfer")
    @ResponseBody
    public String transfer(@RequestParam Long orgId,
                           @RequestParam(required = false) Long toUserId,
                           @RequestParam Long sourceUserId,
                           @RequestParam String clientOrderId,
                           @RequestParam String amount,
                           @RequestParam String tokenId,
                           @RequestParam Integer businessType,
                           @RequestParam(required = false) String address,
                           @RequestParam(required = false) String addressExt) {
        TransferResult result = balanceService.userTransferToUser(Header
                        .builder()
                        .platform(RequestPlatform.PC)
                        .userId(sourceUserId)
                        .orgId(orgId)
                        .build(),
                toUserId, clientOrderId, amount, tokenId, 0, businessType, address, addressExt);
        return ResultUtils.toRestJSONString(result);
    }

//    @RequestMapping(value = "/internal/openapi/withdraw")
//    public void openApiWithdraw(@RequestParam(name = "orgId", required = true) Long orgId,
//                                @RequestParam(name = "userId", required = true) Long userId,
//                                @RequestParam(name = "address", required = true) String address,
//                                @RequestParam(name = "addressExt", required = true) String addressExt,
//                                @RequestParam(name = "tokenId", required = true) String tokenId,
//                                @RequestParam(name = "withdrawQuantity", required = true) String withdrawQuantity,
//                                @RequestParam(name = "chainType", required = true) String chainType,
//                                @RequestParam(name = "isQuick", required = true) Boolean isQuick) {
//        WithdrawResult result
//                = balanceService.openApiWithdraw(Header
//                .builder()
//                .platform(RequestPlatform.PC)
//                .userId(userId)
//                .orgId(orgId)
//                .build(), String.valueOf(sequenceGenerator.getLong()), address, addressExt, tokenId, withdrawQuantity, chainType, Boolean.TRUE, isQuick);
//        log.info("openApiWithdraw {}", JSON.toJSONString(result));
//    }

    @RequestMapping(value = "/internal/openapi/withdraw/detail")
    public void getWithdrawOrderDetail(
            @RequestParam(name = "orgId", required = true) Long orgId,
            @RequestParam(name = "userId", required = true) Long userId,
            @RequestParam(name = "orderId", required = true) Long orderId,
            @RequestParam(name = "clientOrderId", required = true) String clientOrderId) {
        WithdrawDetailResult result = balanceService.getWithdrawOrderDetail(Header
                .builder()
                .platform(RequestPlatform.PC)
                .userId(userId)
                .orgId(orgId)
                .build(), clientOrderId, orderId);
        log.info("getWithdrawOrderDetail {}", JSON.toJSONString(result));
    }

    @RequestMapping(value = "/internal/openapi/create/third/user")
    public void createThirdPartyAccount(
            @RequestParam(name = "orgId", required = true) Long orgId,
            @RequestParam(name = "thirdUserId", required = true) String thirdUserId) {
        Long userId = accountService.createThirdPartyAccount(Header
                .builder()
                .platform(RequestPlatform.PC)
                .orgId(orgId)
                .build(), thirdUserId);
        log.info("createThirdPartyAccount {}", userId);
    }

    @RequestMapping(value = "/internal/openapi/create/third/token")
    public void createThirdPartyToken(
            @RequestParam(name = "orgId", required = true) Long orgId,
            @RequestParam(name = "userId", required = false, defaultValue = "0") Long userId,
            @RequestParam(name = "thirdUserId", required = false, defaultValue = "") String thirdUserId,
            @RequestParam(required = false, defaultValue = "1") Integer accountType) {
        ThirdPartyUserTokenResult result = accountService.createThirdPartyToken(Header
                .builder()
                .platform(RequestPlatform.PC)
                .userId(userId)
                .orgId(orgId)
                .build(), thirdUserId, userId, "PC", accountType);
        log.info("createThirdPartyToken {}", JSON.toJSONString(result));
    }

    @RequestMapping(value = "/internal/openapi/clear/third/token")
    public void clearToken(
            @RequestParam(name = "orgId", required = true) Long orgId,
            @RequestParam(name = "userId", required = false, defaultValue = "0") Long userId,
            @RequestParam(name = "thirdUserId", required = false, defaultValue = "") String thirdUserId,
            @RequestParam(required = false, defaultValue = "PC") String platform) {
        accountService.clearToken(Header
                .builder()
                .platform(RequestPlatform.PC)
                .userId(userId)
                .orgId(orgId)
                .build(), thirdUserId, userId, platform);
        log.info("clearToken success");
    }



}
