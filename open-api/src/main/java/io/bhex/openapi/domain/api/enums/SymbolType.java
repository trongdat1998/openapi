package io.bhex.openapi.domain.api.enums;

public enum SymbolType {

    SPOT("spot");

    private String value;

    SymbolType(String value) {
        this.value = value;
    }

    public String getSymbolType() {
        return value;
    }

    public static SymbolType fromValue(String value) {
        for (SymbolType type : SymbolType.values()) {
            if (type.getSymbolType().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
