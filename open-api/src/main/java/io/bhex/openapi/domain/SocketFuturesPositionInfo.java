package io.bhex.openapi.domain;

import com.google.gson.annotations.SerializedName;
import io.bhex.openapi.domain.api.enums.ApiFuturesPositionSide;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
public class SocketFuturesPositionInfo {

    @SerializedName("e")
    private String eventType;

    @SerializedName("E")
    private Long eventTime;

    @SerializedName("A")
    private String accountId;

    @SerializedName("s")
    private String symbol;

    @SerializedName("S")
    private ApiFuturesPositionSide side;

    @SerializedName("p")
    private String avgPrice;

    @SerializedName("P")
    private String position;

    @SerializedName("a")
    private String available;

    @SerializedName("f")
    private String flp;

    @SerializedName("m")
    private String margin;

    @SerializedName("r")
    private String realizedPnL;
}
