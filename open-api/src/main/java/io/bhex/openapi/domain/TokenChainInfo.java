package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenChainInfo {

    private String chainType;
    private Boolean allowDeposit;
    private Boolean allowWithdraw;

}
