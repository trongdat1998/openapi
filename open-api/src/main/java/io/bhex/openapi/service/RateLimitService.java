package io.bhex.openapi.service;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import io.bhex.broker.common.entity.Header;
import io.bhex.openapi.domain.api.enums.RateLimitInterval;
import io.bhex.openapi.domain.api.enums.RateLimitType;
import io.bhex.openapi.dto.RateLimit;
import io.bhex.openapi.interceptor.annotation.LimitAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class RateLimitService {

    public static final String RATE_LEVEL_PREFIX = "LEVEL-";

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    private static ImmutableTable<String, String, List<RateLimit>> RATE_LIMIT_TABLE = ImmutableTable.of();

    static {
//        Lists.newArrayList(
//                RateLimit.builder()
//                        .rateLimitType(RateLimitType.ORDERS)
//                        .interval(RateLimitInterval.SECOND)
//                        .intervalUnit(1)
//                        .limit(20)
//                        .build(),
//                RateLimit.builder()
//                        .rateLimitType(RateLimitType.ORDERS)
//                        .interval(RateLimitInterval.DAY)
//                        .intervalUnit(1)
//                        .limit(350000)
//                        .build()
//        ));
        RATE_LIMIT_TABLE = ImmutableTable.<String, String, List<RateLimit>>builder()
                .put(RATE_LEVEL_PREFIX + 1, RateLimitType.REQUEST_WEIGHT.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.REQUEST_WEIGHT)
                                        .interval(RateLimitInterval.MINUTE)
                                        .intervalUnit(1)
                                        .limit(1500)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 1, RateLimitType.ORDERS.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.ORDERS)
                                        .interval(RateLimitInterval.SECOND)
                                        .intervalUnit(30)
                                        .limit(30)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 2, RateLimitType.REQUEST_WEIGHT.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.REQUEST_WEIGHT)
                                        .interval(RateLimitInterval.MINUTE)
                                        .intervalUnit(1)
                                        .limit(3000)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 2, RateLimitType.ORDERS.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.ORDERS)
                                        .interval(RateLimitInterval.SECOND)
                                        .intervalUnit(60)
                                        .limit(60)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 3, RateLimitType.REQUEST_WEIGHT.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.REQUEST_WEIGHT)
                                        .interval(RateLimitInterval.MINUTE)
                                        .intervalUnit(1)
                                        .limit(5000)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 3, RateLimitType.ORDERS.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.ORDERS)
                                        .interval(RateLimitInterval.SECOND)
                                        .intervalUnit(2)
                                        .limit(40)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 4, RateLimitType.REQUEST_WEIGHT.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.REQUEST_WEIGHT)
                                        .interval(RateLimitInterval.MINUTE)
                                        .intervalUnit(1)
                                        .limit(8000)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 4, RateLimitType.ORDERS.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.ORDERS)
                                        .interval(RateLimitInterval.SECOND)
                                        .intervalUnit(2)
                                        .limit(100)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 5, RateLimitType.REQUEST_WEIGHT.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.REQUEST_WEIGHT)
                                        .interval(RateLimitInterval.MINUTE)
                                        .intervalUnit(1)
                                        .limit(10000)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 5, RateLimitType.ORDERS.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.ORDERS)
                                        .interval(RateLimitInterval.SECOND)
                                        .intervalUnit(2)
                                        .limit(200)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 6, RateLimitType.REQUEST_WEIGHT.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.REQUEST_WEIGHT)
                                        .interval(RateLimitInterval.MINUTE)
                                        .intervalUnit(1)
                                        .limit(12000)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 6, RateLimitType.ORDERS.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.ORDERS)
                                        .interval(RateLimitInterval.SECOND)
                                        .intervalUnit(2)
                                        .limit(250)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 7, RateLimitType.REQUEST_WEIGHT.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.REQUEST_WEIGHT)
                                        .interval(RateLimitInterval.MINUTE)
                                        .intervalUnit(1)
                                        .limit(15000)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 7, RateLimitType.ORDERS.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.ORDERS)
                                        .interval(RateLimitInterval.SECOND)
                                        .intervalUnit(2)
                                        .limit(300)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 8, RateLimitType.REQUEST_WEIGHT.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.REQUEST_WEIGHT)
                                        .interval(RateLimitInterval.MINUTE)
                                        .intervalUnit(1)
                                        .limit(18000)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 8, RateLimitType.ORDERS.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.ORDERS)
                                        .interval(RateLimitInterval.SECOND)
                                        .intervalUnit(2)
                                        .limit(400)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 9, RateLimitType.REQUEST_WEIGHT.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.REQUEST_WEIGHT)
                                        .interval(RateLimitInterval.MINUTE)
                                        .intervalUnit(1)
                                        .limit(20000)
                                        .build()
                        ))
                .put(RATE_LEVEL_PREFIX + 9, RateLimitType.ORDERS.toString(),
                        Lists.newArrayList(
                                RateLimit.builder()
                                        .rateLimitType(RateLimitType.ORDERS)
                                        .interval(RateLimitInterval.SECOND)
                                        .intervalUnit(2)
                                        .limit(500)
                                        .build()
                        ))
                .build();
    }


    public RateLimit checkRateLimit(Header header, LimitAuth auth, String apiKey, int level, int weight) {
        RateLimitType[] typeArray = auth.limitTypes();
        if (typeArray.length <= 0) {
            return null;
        }
        level = (level == 0) ? 1 : level;
        for (RateLimitType limitType : typeArray) {
            List<RateLimit> rateLimitList = RATE_LIMIT_TABLE.get(RATE_LEVEL_PREFIX + level, limitType.toString());
            if (CollectionUtils.isEmpty(rateLimitList)) {
                continue;
            }

            for (RateLimit limit : rateLimitList) {
                String key = getRateLimitCacheKey(limitType, limit.getInterval(), limit.getIntervalUnit(), apiKey);
                long count = redisTemplate.opsForValue().increment(key, weight);
                if (count > limit.getLimit()) {
                    log.info("request:{}-{}-{} reached rate limit!!! apiKey:{} type:{}  interval:{} limit:{}",
                            header.getOrgId(), header.getUserId(), header.getServerUri(),
                            apiKey, limit.getRateLimitType(), limit.getInterval(), limit.getLimit());
                    redisTemplate.opsForValue().setIfAbsent(getReachedRateLimitCacheKey(key), String.valueOf(limit.getLimit()));
                    resetRateLimitExpire(getReachedRateLimitCacheKey(key), limit.getInterval(), limit.getIntervalUnit()); // 设置key的失效时间
                    return limit;
                }
                resetRateLimitExpire(key, limit.getInterval(), limit.getIntervalUnit()); // 设置一个Key的失效时间
            }
        }
        return null;
    }

    public long getNormalSecondEndTime(int intervalUnit) {
        Calendar calendar = Calendar.getInstance();
        if (intervalUnit > 0) {
            calendar.add(Calendar.SECOND, intervalUnit);
        }
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public long getNormalMinuteEndTime(int intervalUnit) {
        Calendar calendar = Calendar.getInstance();
        if (intervalUnit > 0) {
            calendar.add(Calendar.MINUTE, intervalUnit);
        }
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public long getNormalDayEndTime(int intervalUnit) {
        Calendar calendar = Calendar.getInstance();
        if (intervalUnit > 0) {
            calendar.add(Calendar.DATE, intervalUnit);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public String getRateLimitCacheKey(RateLimitType limitType, RateLimitInterval interval, int intervalUnit, String apiKey) {
        long seconds = System.currentTimeMillis() / 1000;
        long timeKey = seconds;
        switch (interval) {
            case DAY:
                timeKey = seconds / (86400 * intervalUnit);
                break;
            case MINUTE:
                timeKey = seconds / (60 * intervalUnit);
                break;
            case SECOND:
                timeKey = seconds / (1 * intervalUnit);
                break;
            default:
                break;
        }
        return limitType + ":" + interval + ":" + apiKey + ":" + timeKey;
    }

    public String getReachedRateLimitCacheKey(String limitKey) {
        return "LIMIT:" + limitKey;
    }

    public void resetRateLimitExpire(String key, RateLimitInterval interval, int intervalUnit) {
        long time;
        switch (interval) {
            case DAY:
                time = getNormalDayEndTime(intervalUnit);
                break;
            case MINUTE:
                time = getNormalMinuteEndTime(intervalUnit);
                break;
            case SECOND:
                time = getNormalSecondEndTime(intervalUnit);
                break;
            default:
                return;
        }
        redisTemplate.expireAt(key, new Date(time));
    }

    public List<RateLimit> getAllRateLimit() {
        List<RateLimit> list = Lists.newArrayList();
        for (List<RateLimit> limits : RATE_LIMIT_TABLE.row(RATE_LEVEL_PREFIX + 2).values()) {
            if (CollectionUtils.isEmpty(limits)) {
                continue;
            }
            list.addAll(limits);
        }
        return list;
    }

}
