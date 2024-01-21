package io.bhex.openapi.domain.api.result;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartyUserTokenResult {

    private Boolean result;
    private String msg;
    private Long userId;
    private String auToken;
    private Long expireTime;
    private Long accountId;
}
