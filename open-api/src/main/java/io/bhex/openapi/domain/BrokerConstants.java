/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/7/9
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.domain;

public class BrokerConstants {

    public static final String MASK_EMAIL_REG = "(?<=.).(?=[^@]*?.@)"; // (?<=.{3}).(?=.*@) or (?<=.{2}).(?=[^@]*?.@)
    public static final String MASK_MOBILE_REG = "(\\d{3})\\d{4}(\\d{4})"; // \d(?=\d{4})

    public static final String WEB_SOCKET_REQUEST_HEADER = "header";
    public static final String WEB_SOCKET_LISTEN_KEY_HEADER = "listenKey";
//    public static final String WEB_SOCKET_REQUEST_USER_ID = "user_id";
//    public static final String WEB_SOCKET_CURRENT_LISTEN_KEY = "listenKey";

    public static final Integer FUTURES_AMOUNT_PRECISION = 8;

    public static final Integer PC_TOKEN_EFFECTIVE_SECONDS = 8 * 60 * 60;
    public static final Integer APP_TOKEN_EFFECTIVE_SECONDS = 24 * 60 * 60;
    public static final Integer DEFAULT_TOKEN_EFFECTIVE_SECONDS = 24 * 60 * 60;

    public static final String CURRENT_AU_TOKEN = "current_token_%s_%s_%s";
    public static final String LOGIN_NOTICE_CHANNEL = "login";
    public static final String LOGOUT_NOTICE_CHANNEL = "logout";

    public static final String API_KEY_TYPE = ".API_KEY_TYPE";
    public static final int API_KEY_READ_ONLY_TYPE = 0;

    public static final double[] CONTROLLER_TIME_BUCKETS = new double[]{
            5, 10, 20, 50, 75, 100, 200, 500, 1000, 2000, 8000
    };

}
