/**********************************
 *@项目名称: openapi
 *@文件名称: io.bhex.broker.util
 *@Date 2018/9/21
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.util;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class WhiteListUserValidUtil {

    public static final String WHITE_EMAIL_KEY = "white_email";
    public static final String WHITE_MOBILE_KEY = "white_mobile";
    public static final String WHITE_IP_KEY = "white_ip";

    public static final String BLACK_EMAIL_KEY = "black_email";
    public static final String BLACK_MOBILE_KEY = "black_mobile";
    public static final String BLACK_IP_KEY = "black_ip";

    @Resource
    private RedisTemplate<String, String> brokerRedisTemplate;


    public void validWhiteIP(String ip) {
        Boolean hasWhiteConfig = brokerRedisTemplate.hasKey(WHITE_IP_KEY);
        if (hasWhiteConfig != null && hasWhiteConfig
                && !brokerRedisTemplate.opsForHash().hasKey(WHITE_IP_KEY, ip)) {
            throw new BrokerException(BrokerErrorCode.NOT_IN_WHILE_LIST);

        }
        Boolean hasBlackConfig = brokerRedisTemplate.hasKey(BLACK_IP_KEY);
        if (hasBlackConfig != null && hasBlackConfig
                && brokerRedisTemplate.opsForHash().hasKey(BLACK_IP_KEY, ip)) {
            throw new BrokerException(BrokerErrorCode.NOT_IN_WHILE_LIST);
        }
    }

}
