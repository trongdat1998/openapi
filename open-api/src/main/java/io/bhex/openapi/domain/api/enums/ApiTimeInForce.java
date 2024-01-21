package io.bhex.openapi.domain.api.enums;

public enum ApiTimeInForce {
    GTC("GTC"),
    IOC("IOC"),
    FOK("FOK");

    private String timeInForce;

    ApiTimeInForce(String timeInForce) {
        this.timeInForce = timeInForce;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public static ApiTimeInForce formValue(String timeInForce) {
        for (ApiTimeInForce tf : ApiTimeInForce.values()) {
            if (tf.getTimeInForce().equals(timeInForce)) {
                return tf;
            }
        }
        return null;
    }
}
