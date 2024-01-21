package io.bhex.openapi.domain.api.enums;

public enum SymbolStatus {

    TRADING("trading"),
    HALT("halt"),
    BREAK("break");

    private String value;

    SymbolStatus(String value) {
        this.value = value;
    }

    public String getSymbolStatus() {
        return value;
    }

    public static SymbolStatus fromValue(String value) {
        for (SymbolStatus type : SymbolStatus.values()) {
            if (type.getSymbolStatus().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
