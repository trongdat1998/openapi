package io.bhex.openapi.util;

import io.bhex.openapi.domain.BrokerConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ValidateUtils {

    public static final String EMAIL_REG = "^([a-z0-9A-Z]+[_|\\-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";

    public static boolean isEmail(String str) {

        try {
            if (StringUtils.isEmpty(str)) {
                return false;
            }

            return str.matches(EMAIL_REG);
        } catch (Exception e) {
            log.error(" isEmail exception:{}", str, e);
        }
        return false;
    }


    public static String outSensitive(String str) {

        if (isEmail(str)) {
            return emailOutSensitive(str);
        } else {
            return mobileOutSensitive(str);
        }
    }

    public static String mobileOutSensitive(String mobile) {
        if (StringUtils.isEmpty(mobile)) {
            return null;
        }
        return mobile.replaceAll(BrokerConstants.MASK_MOBILE_REG, "$1****$2");
    }

    public static String emailOutSensitive(String email) {
        if (StringUtils.isEmpty(email)) {
            return null;
        }
        return email.replaceAll(BrokerConstants.MASK_EMAIL_REG, "*");
    }

}
