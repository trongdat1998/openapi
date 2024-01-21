package io.bhex.openapi.domain.api.enums;

import com.google.common.base.Strings;

public enum  ApiOrderSide {

    BUY("buy"),
    SELL("sell");

    private String value;

    ApiOrderSide(String name) {
        this.value = name;
    }

    public String value() {
        return this.value;
    }

    public static ApiOrderSide fromValue(String value) {
        String side = Strings.nullToEmpty(value).toUpperCase();
        switch (side) {
            case "BUY":
                return ApiOrderSide.BUY;
            case "SELL":
                return ApiOrderSide.SELL;
            default:
                throw null;
        }
    }

}
