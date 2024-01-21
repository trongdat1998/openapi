package io.bhex.openapi.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WithdrawResult {
    private Boolean success;
    private Boolean needBrokerAudit;
    private Long orderId;
    private Boolean allowWithdraw;
    private String refuseReason;
}
