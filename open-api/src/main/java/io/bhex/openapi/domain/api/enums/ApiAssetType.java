package io.bhex.openapi.domain.api.enums;

public enum ApiAssetType {

    CASH("cash"),
    MARGIN("margin");

    private String assetType;

    ApiAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetType() {
        return assetType;
    }

    public static ApiAssetType fromValue(String assetType) {
        for (ApiAssetType type : ApiAssetType.values()) {
            if (type.getAssetType().equals(assetType)) {
                return type;
            }
        }
        return null;
    }
}
