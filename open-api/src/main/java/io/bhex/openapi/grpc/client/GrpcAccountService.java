/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.grpc.client
 *@Date 2018/6/25
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.grpc.client;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.common.grpc.client.annotation.GrpcLog;
import io.bhex.broker.common.grpc.client.annotation.PrometheusMetrics;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.broker.grpc.account.*;
import io.bhex.broker.grpc.common.Header;
import io.bhex.broker.grpc.deposit.DepositServiceGrpc;
import io.bhex.broker.grpc.deposit.GetDepositAddressRequest;
import io.bhex.broker.grpc.deposit.GetDepositAddressResponse;
import io.bhex.broker.grpc.withdraw.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcLog
@PrometheusMetrics
public class GrpcAccountService extends GrpcBaseService {

    public QueryAccountResponse queryAccount(Header header, QueryAccountRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QueryAccountResponse response = stub.queryAccounts(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetAccountResponse getAccount(Header header, GetAccountRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            GetAccountResponse response = stub.getAccount(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public QueryBalanceResponse queryBalance(Header header, QueryBalanceRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            if (!request.hasHeader()) {
                request = request.toBuilder().setHeader(header).build();
            }
            QueryBalanceResponse response = stub.queryBalance(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @GrpcLog(printNoResponse = true)
    public QueryBalanceFlowResponse queryBalanceFlow(QueryBalanceFlowRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            QueryBalanceFlowResponse response = stub.queryBalanceFlow(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    // 获取资产余额信息
//    public GetAllAssetInfoResponse getAllAssetInfo(GetAllAssetInfoRequest request) {
//        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
//        try {
//            GetAllAssetInfoResponse response = stub.getAllAssetInfo(request);
//            if (response.getRet() != 0) {
//                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
//            }
//            return response;
//        } catch (StatusRuntimeException e) {
//            log.error("{}", printStatusRuntimeException(e));
//            throw commonStatusRuntimeException(e);
//        }
//    }

    public OptionAssetListResponse getOptionAssetList(OptionAssetRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            OptionAssetListResponse response = stub.getOptionAssetList(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    // 获取期权 数字货币资产余额
    public OptionAccountDetailListResponse getOptionAccountDetail(OptionAccountDetailRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            OptionAccountDetailListResponse response = stub.getOptionAccountDetail(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public BalanceTransferResponse balanceTransfer(BalanceTransferRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            BalanceTransferResponse response = stub.balanceTransfer(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public InstitutionalUserTransferResponse institutionalUserTransfer(InstitutionalUserTransferRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            InstitutionalUserTransferResponse response = stub.institutionalUserTransfer(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @GrpcLog(printNoResponse = true)
    public QuerySubAccountResponse queryAllSubAccount(QueryAllSubAccountRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            QuerySubAccountResponse response = stub.queryAllSubAccount(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public SubAccountTransferResponse subAccountTransfer(SubAccountTransferRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            SubAccountTransferResponse response = stub.subAccountTransfer(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetDepositAddressResponse getDepositAddress(GetDepositAddressRequest request) {
        DepositServiceGrpc.DepositServiceBlockingStub stub = grpcClientConfig.depositServiceBlockingStub();
        try {
            GetDepositAddressResponse response = stub.getDepositAddress(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public OpenApiWithdrawResponse openApiWithdraw(OpenApiWithdrawRequest request) {
        WithdrawServiceGrpc.WithdrawServiceBlockingStub stub = grpcClientConfig.withdrawServiceBlockingStub();
        try {
            OpenApiWithdrawResponse response = stub.openApiWithdraw(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetWithdrawOrderDetailResponse getWithdrawOrderDetail(GetWithdrawOrderDetailRequest request) {
        WithdrawServiceGrpc.WithdrawServiceBlockingStub stub = grpcClientConfig.withdrawServiceBlockingStub();
        try {
            GetWithdrawOrderDetailResponse response = stub.getWithdrawOrderDetail(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public GetThirdPartyAccountResponse getThirdPartyAccount(GetThirdPartyAccountRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            GetThirdPartyAccountResponse response = stub.getThirdPartyAccount(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CreateThirdPartyAccountResponse createThirdPartyAccount(CreateThirdPartyAccountRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            CreateThirdPartyAccountResponse response = stub.createThirdPartyAccount(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public CreateThirdPartyTokenResponse createThirdPartyToken(CreateThirdPartyTokenRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            CreateThirdPartyTokenResponse response = stub.createThirdPartyToken(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public ThirdPartyUserTransferInResponse thirdPartyUserTransferIn(ThirdPartyUserTransferInRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            ThirdPartyUserTransferInResponse response = stub.thirdPartyUserTransferIn(request);
            log.info("thirdPartyUserTransferIn request:{}, response:{}",
                    JsonUtil.defaultGson().toJson(request), JsonUtil.defaultGson().toJson(response));
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    public ThirdPartyUserTransferOutResponse thirdPartyUserTransferOut(ThirdPartyUserTransferOutRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            ThirdPartyUserTransferOutResponse response = stub.thirdPartyUserTransferOut(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }

    @GrpcLog(printNoResponse = true)
    public ThirdPartyUserBalanceResponse thirdPartyUserBalance(ThirdPartyUserBalanceRequest request) {
        AccountServiceGrpc.AccountServiceBlockingStub stub = grpcClientConfig.accountServiceBlockingStub();
        try {
            ThirdPartyUserBalanceResponse response = stub.thirdPartyUserBalance(request);
            if (response.getRet() != 0) {
                throw new BrokerException(BrokerErrorCode.fromCode(response.getRet()));
            }
            return response;
        } catch (StatusRuntimeException e) {
            log.error("{}", printStatusRuntimeException(e));
            throw commonStatusRuntimeException(e);
        }
    }
}
