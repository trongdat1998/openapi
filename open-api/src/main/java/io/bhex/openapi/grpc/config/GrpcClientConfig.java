/*
 ************************************
 * @项目名称: broker
 * @文件名称: GrcpClientConfig
 * @Date 2018/05/22
 * @Author will.zhao@bhex.io
 * @Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 **************************************
 */
package io.bhex.openapi.grpc.config;

import com.google.common.collect.Maps;
import io.bhex.base.account.FuturesServerGrpc;
import io.bhex.base.account.SubscribeServiceGrpc;
import io.bhex.base.grpc.client.channel.IGrpcClientPool;
import io.bhex.base.quote.QuoteServiceGrpc;
import io.bhex.broker.common.entity.GrpcChannelInfo;
import io.bhex.broker.grpc.account.AccountServiceGrpc;
import io.bhex.broker.grpc.basic.BasicServiceGrpc;
import io.bhex.broker.grpc.broker.BrokerConfigServiceGrpc;
import io.bhex.broker.grpc.broker.BrokerServiceGrpc;
import io.bhex.broker.grpc.common_ini.CommonIniServiceGrpc;
import io.bhex.broker.grpc.deposit.DepositServiceGrpc;
import io.bhex.broker.grpc.finance_support.FinanceSupportServiceGrpc;
import io.bhex.broker.grpc.gateway.BrokerAuthServiceGrpc;
import io.bhex.broker.grpc.margin.MarginOrderServiceGrpc;
import io.bhex.broker.grpc.margin.MarginPositionServiceGrpc;
import io.bhex.broker.grpc.margin.MarginServiceGrpc;
import io.bhex.broker.grpc.order.FuturesOrderServiceGrpc;
import io.bhex.broker.grpc.order.OrderServiceGrpc;
import io.bhex.broker.grpc.otc.third.party.OtcThirdPartyServiceGrpc;
import io.bhex.broker.grpc.security.SecurityServiceGrpc;
import io.bhex.broker.grpc.transfer.TransferServiceGrpc;
import io.bhex.broker.grpc.user.UserSecurityServiceGrpc;
import io.bhex.broker.grpc.user.UserServiceGrpc;
import io.bhex.broker.grpc.withdraw.WithdrawServiceGrpc;
import io.bhex.openapi.BrokerProperties;
import io.grpc.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GrpcClientConfig {

    public static final String BH_SERVER_CHANNEL_NAME = "bhServer";
    public static final String BH_SUB_SERVER_CHANNEL_NAME = "bhSubServer";
    public static final String BROKER_SERVER_CHANNEL_NAME = "brokerServer";
    public static final String SECURITY_SERVER_CHANNEL_NAME = "securityServer";
    public static final String QUOTE_SERVER_CHANNEL_NAME = "quoteDataServer";

    @Resource
    private BrokerProperties brokerProperties;

    @Resource
    private IGrpcClientPool pool;

    Long stubDeadline;

    @PostConstruct
    public void init() throws Exception {
        stubDeadline = brokerProperties.getGrpcClient().getStubDeadline();
        //featureTimeout = brokerProperties.getGrpcClient().getFutureTimeout();

        List<GrpcChannelInfo> channelInfoList = brokerProperties.getGrpcClient().getChannelInfo();
        Map<String, Channel> channelMap = Maps.newHashMap();

        for (GrpcChannelInfo channelInfo : channelInfoList) {
            pool.setShortcut(channelInfo.getChannelName(), channelInfo.getHost(), channelInfo.getPort());
        }
//        channelMap = ImmutableMap.copyOf(channelMap);
    }

    public BrokerServiceGrpc.BrokerServiceBlockingStub brokerServiceBlockingStub() {
//        Channel channel = channelMap.get(BROKER_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return BrokerServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }


    public BasicServiceGrpc.BasicServiceBlockingStub basicServiceBlockingStub() {
//        Channel channel = channelMap.get(BROKER_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return BasicServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public AccountServiceGrpc.AccountServiceBlockingStub accountServiceBlockingStub() {
//        Channel channel = channelMap.get(BROKER_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return AccountServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public WithdrawServiceGrpc.WithdrawServiceBlockingStub withdrawServiceBlockingStub() {
//        Channel channel = channelMap.get(BROKER_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return WithdrawServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public UserServiceGrpc.UserServiceBlockingStub userServiceBlockingStub() {
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return UserServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public UserSecurityServiceGrpc.UserSecurityServiceBlockingStub userSecurityServiceBlockingStub() {
//        Channel channel = channelMap.get(BROKER_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return UserSecurityServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public OrderServiceGrpc.OrderServiceBlockingStub orderServiceBlockingStub() {
//        Channel channel = channelMap.get(BROKER_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return OrderServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public DepositServiceGrpc.DepositServiceBlockingStub depositServiceBlockingStub() {
//        Channel channel = channelMap.get(BROKER_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return DepositServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public SecurityServiceGrpc.SecurityServiceBlockingStub securityServiceBlockingStub() {
//        Channel channel = channelMap.get(SECURITY_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(SECURITY_SERVER_CHANNEL_NAME);
        return SecurityServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public SubscribeServiceGrpc.SubscribeServiceStub subscribeServiceStub(Long orgId) {
//        Channel channel = channelMap.get(BH_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(BH_SUB_SERVER_CHANNEL_NAME);
        return SubscribeServiceGrpc.newStub(channel);
    }

    public SubscribeServiceGrpc.SubscribeServiceBlockingStub subscribeServiceBlockingStub(Long orgId) {
//        Channel channel = channelMap.get(BH_SERVER_CHANNEL_NAME);
        Channel channel = pool.robChannel(BH_SUB_SERVER_CHANNEL_NAME);
        return SubscribeServiceGrpc.newBlockingStub(channel);
    }

    public FuturesServerGrpc.FuturesServerBlockingStub futuresServerBlockingStub(Long orgId) {
        Channel channel = pool.borrowChannel(BH_SERVER_CHANNEL_NAME);
        return FuturesServerGrpc.newBlockingStub(channel);
    }

    public BrokerConfigServiceGrpc.BrokerConfigServiceBlockingStub brokerConfigServiceBlockingStub() {
        //        Channel channel = channelMap.get(BROKER_SERVER_CHANNEL_NAME);
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return BrokerConfigServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public TransferServiceGrpc.TransferServiceBlockingStub transferServiceBlockingStub() {
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return TransferServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public FuturesOrderServiceGrpc.FuturesOrderServiceBlockingStub futuresOrderServiceBlockingStub() {
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return FuturesOrderServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public CommonIniServiceGrpc.CommonIniServiceBlockingStub commonIniServiceBlockingStub() {
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return CommonIniServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public FinanceSupportServiceGrpc.FinanceSupportServiceBlockingStub financeSupportServiceBlockingStub() {
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return FinanceSupportServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public BrokerAuthServiceGrpc.BrokerAuthServiceBlockingStub brokerAuthServiceBlockingStub() {
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return BrokerAuthServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public MarginServiceGrpc.MarginServiceBlockingStub marginServiceBlockingStub() {
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return MarginServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public QuoteServiceGrpc.QuoteServiceBlockingStub quoteServiceBlockingStub(Long orgId) {
        Channel channel = pool.borrowChannel(QUOTE_SERVER_CHANNEL_NAME);
        return QuoteServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);
    }

    public MarginPositionServiceGrpc.MarginPositionServiceBlockingStub marginPositionServiceBlockingStub() {
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return MarginPositionServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);

    }

    public OtcThirdPartyServiceGrpc.OtcThirdPartyServiceBlockingStub otcThirdPartyServiceBlockingStub() {
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return OtcThirdPartyServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);

    }

    public MarginOrderServiceGrpc.MarginOrderServiceBlockingStub marginOrderServiceBlockingStub(){
        Channel channel = pool.borrowChannel(BROKER_SERVER_CHANNEL_NAME);
        return MarginOrderServiceGrpc.newBlockingStub(channel).withDeadlineAfter(stubDeadline, TimeUnit.MILLISECONDS);

    }
}


