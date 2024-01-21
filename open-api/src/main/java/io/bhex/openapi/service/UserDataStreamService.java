package io.bhex.openapi.service;

import com.google.gson.reflect.TypeToken;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.broker.grpc.common.AccountTypeEnum;
import io.bhex.broker.grpc.user.GetUserInfoRequest;
import io.bhex.broker.grpc.user.GetUserInfoResponse;
import io.bhex.broker.grpc.user.Index0AccountIds;
import io.bhex.openapi.domain.ValidListenKeyResult;
import io.bhex.openapi.domain.websocket.WebSocketSessionProxy;
import io.bhex.openapi.grpc.client.GrpcUserService;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserDataStreamService {

    public static final String LISTEN_KEY_HEADER = "wsListenKey:";

    //60分钟
    public static final long LISTEN_KEY_EXPIRE_TIME = 60;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private GrpcUserService grpcUserService;

    public ValidListenKeyResult validListenKey(String listenKey) {
        if(listenKey.startsWith("perf_test")) {
            String[] arr = listenKey.split("-");
            Long start = Long.valueOf(arr[1]);
            Long end = Long.valueOf(arr[2]);
            Long[] accountIds = new Long[(int) (end-start+1)];
            for (long accountId = start; accountId <= end; accountId++) {
                accountIds[(int)(accountId-start)] = accountId;
            }
            return ValidListenKeyResult.builder()
                    .accountIds(accountIds)
                    .listenKey(listenKey)
                    .orgId(6001L)
                    .accountTypeEnum(AccountTypeEnum.COIN)
                    .build();
        }
        String cacheString = redisTemplate.opsForValue().get(getCacheKey(listenKey));
        if (StringUtils.isEmpty(cacheString)) {
            return null;
        }
        try {
            return JsonUtil.defaultGson().fromJson(cacheString, new TypeToken<ValidListenKeyResult>() {
            }.getType());
        } catch (Exception e) {
            log.error(" validListenKey failed: key:{}", listenKey, e);
        }
        return null;
    }

    public String createListenKey(Header header, Long accountId, AccountTypeEnum accountType, Integer accountIndex) {
        String key = null;
        // 随机生成一个listenKey 64位长度
        while (true) {
            key = RandomStringUtils.randomAlphabetic(64);
            if (redisTemplate.opsForValue().setIfAbsent(getCacheKey(key), "", LISTEN_KEY_EXPIRE_TIME, TimeUnit.MINUTES)) {
                Long[] accountIds = null;
                if (accountId != null && accountId != 0) {
                    accountIds = new Long[]{accountId};
                } else {
                    GetUserInfoResponse response
                            = grpcUserService.getUserInfo(GetUserInfoRequest.newBuilder().setHeader(HeaderConvertUtil.convertHeader(header)).build());
                    Index0AccountIds defaultIndex0AccountIds = response.getUser().getDefaultIndex0AccountIds();
                    accountIds = new Long[]{defaultIndex0AccountIds.getCoinIndex0AccountId(), defaultIndex0AccountIds.getOptionalIndex0AccountId(),
                            defaultIndex0AccountIds.getFuturesIndex0AccountId(),defaultIndex0AccountIds.getMarginIndex0AccountId()};
                }
                ValidListenKeyResult listenKeyResult = ValidListenKeyResult.builder()
                        .listenKey(key)
                        .orgId(header.getOrgId())
                        .userId(header.getUserId())
                        .accountTypeEnum(accountType)
                        .accountIndex(accountIndex)
                        .accountIds(accountIds)
                        .build();
                redisTemplate.opsForValue().set(getCacheKey(key), JsonUtil.defaultGson().toJson(listenKeyResult));
                log.info("create listenKey: key:{} -> {}", key, JsonUtil.defaultGson().toJson(listenKeyResult));
                break;
            }
        }
        return key;
    }

    public int updateListenKey(String listenKey) {
        String cacheString = redisTemplate.opsForValue().get(getCacheKey(listenKey));
        if (StringUtils.isEmpty(cacheString)) {
            return -1;
        }
        // 设置超时时间 60 分钟
        redisTemplate.expire(getCacheKey(listenKey), LISTEN_KEY_EXPIRE_TIME, TimeUnit.MINUTES);
        ValidListenKeyResult listenKeyResult = JsonUtil.defaultGson().fromJson(cacheString, new TypeToken<ValidListenKeyResult>() {
        }.getType());
        for (Long accountId : listenKeyResult.getAccountIds()) {
            WebSocketSessionProxy.extendSession(accountId);
        }
        return 1;
    }


    public int deleteListenKey(Header header, String listenKey) {
        String cacheString = redisTemplate.opsForValue().get(getCacheKey(listenKey));
        // 获取缓存， 若缓存不存在， 则认为直接是成功的
        if (StringUtils.isEmpty(cacheString)) {
            return 1;
        }
        // 删除缓存
        redisTemplate.delete(getCacheKey(listenKey));
        return 1;
    }

    public String getCacheKey(String listenKey) {
        return LISTEN_KEY_HEADER + listenKey;
    }

}
