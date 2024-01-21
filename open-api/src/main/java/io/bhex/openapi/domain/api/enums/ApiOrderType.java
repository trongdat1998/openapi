package io.bhex.openapi.domain.api.enums;

import com.google.common.base.Strings;

public enum ApiOrderType {

    MARKET("market"),
    LIMIT("limit"),
    LIMIT_MAKER("limit_maker"),
    COM("com"),
    LOCAL_ONLY("local_only"),
    STOP_LOSS("stop_loss"),
    STOP_LOSS_LIMIT("stop_loss_limit"),
    TAKE_PROFIT("take_profit"),
    TAKE_PROFIT_LIMIT("take_profit_limit"),
    MARKET_OF_PAYOUT("market_of_payout"),
    LIMIT_FREE("limit_free"),
    LIMIT_MAKER_FREE("limit_maker_free")
    ;

    private String value;

    ApiOrderType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public static ApiOrderType fromValue(String value) {
        String side = Strings.nullToEmpty(value).toUpperCase();
        switch (side) {
            case "MARKET":
                return ApiOrderType.MARKET;
            case "LIMIT":
                return ApiOrderType.LIMIT;
            case "LIMIT_MAKER":
                return ApiOrderType.LIMIT_MAKER;
            case "COM":
                return ApiOrderType.COM;
            case "LOCAL_ONLY":
                return ApiOrderType.LOCAL_ONLY;
            case "LIMIT_FREE":
                return ApiOrderType.LIMIT_FREE;
            case "LIMIT_MAKER_FREE":
                return ApiOrderType.LIMIT_MAKER_FREE;
            default:
                throw null;
        }
    }
}
