package io.bhex.openapi.domain.api.enums;

public enum OrderStatus {

    NEW("new"),
    PARTIALLY_FILLED("partially_filled"),
    FILLED("filled"),
    CANCELED("canceled"),
    PENDING_CANCEL("pending_cancel"),
    REJECTED("reject"),;

    private String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getOrderStatus() {
        return value;
    }

    public static OrderStatus fromValue(String value) {
        for (OrderStatus type : OrderStatus.values()) {
            if (type.getOrderStatus().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
