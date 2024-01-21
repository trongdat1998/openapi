package io.bhex.openapi.domain;

import io.bhex.broker.grpc.common.AccountTypeEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidListenKeyResult {

    private String listenKey;
    private Long orgId;
    private Long userId;
    private AccountTypeEnum accountTypeEnum;
    private Integer accountIndex;
    private Long[] accountIds;

}
