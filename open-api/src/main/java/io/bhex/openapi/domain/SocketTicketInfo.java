package io.bhex.openapi.domain;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
public class SocketTicketInfo {

    @SerializedName("e")
    private String eventType;

    @SerializedName("E")
    private Long eventTime;

    @SerializedName("s")
    private String symbol;

    @SerializedName("q")
    private String quantity;

    @SerializedName("t")
    private Long time;

    @SerializedName("p")
    private String price;

    @SerializedName("T")
    private Long ticketId;

    @SerializedName("o")
    private Long orderId;

    @SerializedName("c")
    private String clientOrderId;

    @SerializedName("O")
    private Long matchOrderId;

    @SerializedName("a")
    private Long accountId;

    @SerializedName("A")
    private Long matchAccountId;
}
