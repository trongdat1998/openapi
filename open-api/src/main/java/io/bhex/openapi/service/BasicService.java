/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.service
 *@Date 2018/6/25
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.service;

import com.google.api.client.util.Sets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.bhex.base.account.GetTotalPositionReply;
import io.bhex.base.account.GetTotalPositionRequest;
import io.bhex.base.proto.BaseRequest;
import io.bhex.base.proto.DecimalUtil;
import io.bhex.base.quote.*;
import io.bhex.base.token.TokenTypeEnum;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.broker.grpc.basic.Token;
import io.bhex.broker.grpc.basic.*;
import io.bhex.broker.grpc.broker.Broker;
import io.bhex.broker.grpc.broker.QueryBrokerRequest;
import io.bhex.broker.grpc.broker.QueryBrokerResponse;
import io.bhex.broker.grpc.margin.*;
import io.bhex.broker.grpc.order.FundingRate;
import io.bhex.broker.grpc.order.GetFundingRatesRequest;
import io.bhex.broker.grpc.order.GetFundingRatesResponse;
import io.bhex.openapi.domain.*;
import io.bhex.openapi.domain.TokenChainInfo;
import io.bhex.openapi.domain.api.result.ContractsResult;
import io.bhex.openapi.domain.api.result.TokenFuturesResult;
import io.bhex.openapi.grpc.client.*;
import io.bhex.openapi.util.FuturesTools;
import io.bhex.openapi.util.HeaderConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BasicService {

    private static final String FUTURES_ACCOUNT_WHITELIST_KEY = "openapi_futures_account_white_list";
    private static final String COIN_ACCOUNT_WHITELIST_KEY = "openapi_coin_account_white_list";

    private static final String FUTURES_USER_WHITELIST_KEY = "openapi_futures_user_white_list";
    private static final String COIN_USER_WHITELIST_KEY = "openapi_coin_user_white_list";

    private static ImmutableMap<String, BrokerInfo> domainBrokerInfoMap = ImmutableMap.of();
    private static ImmutableMap<Long, BrokerInfo> orgIdBrokerInfoMap = ImmutableMap.of();

    private static ImmutableMap<Long, List<TokenResult>> orgTokenMap = ImmutableMap.of();
    private static ImmutableMap<Long, List<SymbolResult>> orgSymbolMap = ImmutableMap.of();

    private static ImmutableMap<Long, List<OptionTokenResult>> orgTokenOptionMap = ImmutableMap.of();
    private static ImmutableMap<Long, List<SymbolResult>> orgFuturesSymbolMap = ImmutableMap.of();

    private static ImmutableMap<Long, Long> exchangeIdMaps = ImmutableMap.of();

    private static Cache<Long, List<SymbolResult>> orgSymbolCache = CacheBuilder.newBuilder().
            expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(200L).build();

    private static ImmutableMap<String, List<Long>> accountWhiteListMap = ImmutableMap.of();
    private static ImmutableMap<String, List<Long>> userWhiteListMap = ImmutableMap.of();

    //杠杆币对配置
    private static ImmutableMap<Long, List<TokenConfig>> orgMarginTokenMap = ImmutableMap.of();
    //杠杆利息配置
    private static ImmutableMap<Long, List<InterestConfig>> orgMarginInterestMap = ImmutableMap.of();

    @Resource
    private GrpcQuoteService grpcQuoteService;

    @Resource
    private GrpcMarginService grpcMarginService;

    @Resource
    private GrpcBasicService grpcBasicService;

    @Resource
    private GrpcFuturesOrderService grpcFuturesOrderService;

    @Resource
    GrpcFuturesBHServerService grpcFuturesBHServerService;


    private static ImmutableMap<Long, RiskConfig> orgMarginRiskMap = ImmutableMap.of();

    private static Cache<String, ContractsResult> contractResultCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS).build();

    private static Cache<Long, Map<String, String>> indexCaches = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS).build();

    private static Cache<Long, Map<String, String>> openInterestCaches = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).build();

    private static Cache<Long, Map<String, FundingRate>> fundingRateCaches = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS).build();


    @PostConstruct
    @Scheduled(cron = "0/5 * * * * ?")
    public void initBasicCache() {
        io.bhex.broker.grpc.common.Header header = io.bhex.broker.grpc.common.Header.getDefaultInstance();
        initBrokers(header);
        initSymbol(header);
        initToken(header);
        initOptionToken(header);
        initFuturesSymbol(header);
        initAccountWhiteLists();
        initUserWhiteLists();
        initMarginConfig();
    }

    public void initBrokers(io.bhex.broker.grpc.common.Header header) {
        QueryBrokerResponse response = grpcBasicService.queryBrokers(header, QueryBrokerRequest.getDefaultInstance());
        List<Broker> brokerList = response.getBrokersList();
        Map<Long, BrokerInfo> orgIdBrokerMap = Maps.newHashMap();
        Map<String, BrokerInfo> tmpBrokerMap = Maps.newHashMap();
        if (brokerList.size() > 0) {
            for (Broker broker : brokerList) {
                orgIdBrokerMap.put(broker.getOrgId(), getBrokerInfo(broker, ""));
                for (int index = 0; index < broker.getApiDomainsCount(); index++) {
                    tmpBrokerMap.put(broker.getApiDomains(index), getBrokerInfo(broker, broker.getApiDomains(index)));
                }
            }
            orgIdBrokerInfoMap = ImmutableMap.copyOf(orgIdBrokerMap);
            domainBrokerInfoMap = ImmutableMap.copyOf(tmpBrokerMap);
        }
    }

    private void initSymbol(io.bhex.broker.grpc.common.Header header) {
        try {
//            QuerySymbolResponse response = grpcBasicService.querySymbols(header, QuerySymbolRequest.newBuilder().addAllCategory(Arrays.asList(1, 2, 3, 4, 5)).build());
//            List<Symbol> symbolList = response.getSymbolList();
            List<Symbol> symbolList = Lists.newCopyOnWriteArrayList();
            for (Long orgId : orgIdBrokerInfoMap.keySet()) {
                symbolList.addAll(grpcBasicService.querySymbols(io.bhex.broker.grpc.common.Header.newBuilder().setOrgId(orgId).build(),
                        QuerySymbolRequest.newBuilder().addAllCategory(Arrays.asList(1, 2, 3, 4, 5)).build()).getSymbolList());
            }
            for (Symbol symbol : symbolList) {
                if (symbol.getCategory() == 5) {
                    log.info("catch aggregate symbol:{}", JsonUtil.defaultGson().toJson(symbol));
                }
            }
            if (symbolList.size() > 0) {
                List<SymbolResult> symbolResultList = symbolList.stream().map(this::getSymbolResult).collect(Collectors.toList());
                Map<Long, List<SymbolResult>> symbolMap = symbolResultList.stream().collect(Collectors.groupingBy(SymbolResult::getOrgId));
                orgSymbolMap = ImmutableMap.copyOf(ImmutableMap.copyOf(symbolMap));
            }
        } catch (Exception e) {
            log.error("refresh symbol cache data error!", e);
        }
    }

    private void initToken(io.bhex.broker.grpc.common.Header header) {
        try {
            List<Token> tokenList = new ArrayList<>();
            orgIdBrokerInfoMap.keySet().forEach(key -> {
                tokenList.addAll(grpcBasicService.queryCoinTokens(io.bhex.broker.grpc.common.Header.newBuilder().setOrgId(key).build()));
            });
            if (tokenList != null && tokenList.size() > 0) {
                List<TokenResult> tokenResultList = tokenList.stream().map(this::getTokenResult).collect(Collectors.toList());
                Map<Long, List<TokenResult>> orgTokenMap = tokenResultList.stream().collect(Collectors.groupingBy(TokenResult::getOrgId));
                BasicService.orgTokenMap = ImmutableMap.copyOf(orgTokenMap);
            }
        } catch (Exception e) {
            log.error("refresh token cache data error!", e);
        }
    }

    public List<TokenResult> queryTokenList(Header header) {
        return orgTokenMap.getOrDefault(header.getOrgId(), Lists.newArrayList());
    }

    public List<SymbolResult> querySymbolList(Header header) {
        return orgSymbolMap.getOrDefault(header.getOrgId(), Lists.newArrayList());
//        List<SymbolResult> symboles = orgSymbolCache.getIfPresent(header.getOrgId());
//        if (!CollectionUtils.isEmpty(symboles)) {
//            return symboles;
//        }
//
//        io.bhex.broker.grpc.common.Header grpcHeader = null;
//        if (null == header) {
//            grpcHeader = io.bhex.broker.grpc.common.Header.getDefaultInstance();
//        } else {
//            grpcHeader = HeaderConvertUtil.convertHeader(header);
//        }
//
//        initSymbol(grpcHeader);
//
//        symboles = orgSymbolCache.getIfPresent(header.getOrgId());
//        if (!CollectionUtils.isEmpty(symboles)) {
//            return symboles;
//        }
//
//        orgSymbolCache.put(header.getOrgId(), Lists.newArrayList());
//        return Lists.newArrayList();
    }

    public SymbolResult querySymbol(Header header, String symbol) {
        if (StringUtils.isEmpty(symbol)) {
            return null;
        }

        List<SymbolResult> symbolResultList = this.querySymbolList(header);
        if (CollectionUtils.isEmpty(symbolResultList)) {
            return null;
        }

        for (SymbolResult result : symbolResultList) {
            if (result.getSymbolId().equalsIgnoreCase(symbol) || result.getSymbolName().equalsIgnoreCase(symbol)) {
                return result;
            }
        }
        return null;
    }

    private TokenResult getTokenResult(Token token) {
        return TokenResult.builder()
                .orgId(token.getOrgId())
                .tokenId(token.getTokenId())
                .tokenName(token.getTokenName())
                .tokenFullName(token.getTokenFullName())
//                .iconUrl(token.getIconUrl())
                .allowDeposit(token.getAllowDeposit())
                .allowWithdraw(token.getAllowWithdraw())
                .chainTypes(token.getTokenChainInfoList().stream()
                        .map(chainInfo -> TokenChainInfo.builder()
                                .chainType(chainInfo.getChainType())
                                .allowDeposit(chainInfo.getAllowDeposit())
                                .allowWithdraw(chainInfo.getAllowWithdraw())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private SymbolResult getSymbolResult(Symbol symbol) {
        SymbolResult symbolResult = SymbolResult.builder()
                .orgId(symbol.getOrgId())
                .exchangeId(symbol.getExchangeId())
                .symbolId(symbol.getSymbolId())
                .symbolName(symbol.getSymbolName())
                .baseTokenId(symbol.getBaseTokenId())
                .baseTokenName(symbol.getBaseTokenName())
                .quoteTokenId(symbol.getQuoteTokenId())
                .quoteTokenName(symbol.getQuoteTokenName())
                .basePrecision(symbol.getBasePrecision())
                .quotePrecision(symbol.getQuotePrecision())
                .minTradeQuantity(symbol.getMinTradeQuantity())
                .minTradeAmount(symbol.getMinTradeAmount())
                .minPricePrecision(symbol.getMinPricePrecision())
                .digitMerge(symbol.getDigitMerge())
                .canTrade(symbol.getCanTrade())
                .category(symbol.getCategory())
                .allowMargin(symbol.getAllowMargin())
                .hideFromOpenapi(symbol.getHideFromOpenapi())
                .forbidOpenapiTrade(symbol.getForbidOpenapiTrade())
                .isAggregate(symbol.getIsAggragate())
                .build();

        if (symbol.hasTokenFutures()) {
            symbolResult.setTokenFutures(FuturesTools.toTokenFutures(symbol));
        }

        symbolResult.setIsReverse(symbol.getIsReverse());
        return symbolResult;
    }

    public BrokerInfo getByRequestHost(String requestDomain) {
        for (String domain : domainBrokerInfoMap.keySet()) {
            if (requestDomain.endsWith(domain)) {
                return domainBrokerInfoMap.get(domain);
            }
        }
        return null;
    }

    public List<BrokerInfo> queryAllBroker() {
        return domainBrokerInfoMap.values().asList();
    }

    private BrokerInfo getBrokerInfo(Broker broker, String domain) {
        return BrokerInfo.builder()
                .id(broker.getId())
                .orgId(broker.getOrgId())
                .brokerName(broker.getBrokerName())
                .brokerDomain(domain)
                .build();
    }

    public Long getServerTime(Header header, Long serverSleeps) {
        GetServerTimeRequest request = GetServerTimeRequest.newBuilder().setServerSleepTime(serverSleeps).build();
        return grpcBasicService.getServerTime(HeaderConvertUtil.convertHeader(header), request);
    }

    private void initOptionToken(io.bhex.broker.grpc.common.Header header) {
        try {
            List<Token> tokenList = Lists.newArrayList();
            for (Long orgId : orgIdBrokerInfoMap.keySet()) {
                QueryTokenResponse queryTokenResponse = grpcBasicService.queryOptionTokens(io.bhex.broker.grpc.common.Header.newBuilder().setOrgId(orgId).build());
                tokenList.addAll(queryTokenResponse.getTokensList());
            }
            if (tokenList.size() > 0) {
                List<OptionTokenResult> tokenResultList = tokenList.stream().map(this::getOptionTokenResult).collect(Collectors.toList());
                Map<Long, List<OptionTokenResult>> orgTokenMap = tokenResultList.stream()
                        .collect(Collectors.groupingBy(OptionTokenResult::getOrgId));
                BasicService.orgTokenOptionMap = ImmutableMap.copyOf(orgTokenMap);
            }
        } catch (Exception e) {
            log.error("refresh token cache data error!", e);
        }
    }

    public List<TokenOptionResult> getOptionTokens(Header header, boolean expired) {
        List<OptionTokenResult> tokenResults
                = orgTokenOptionMap.getOrDefault(header.getOrgId(), Lists.newArrayList());
        if (tokenResults.size() == 0) {
            return new ArrayList<>();
        }
        //过滤是否过期
        if (expired) {
            return tokenResults
                    .stream()
                    .filter(option -> option.getBaseTokenOption()
                            .getExpiration() < new Date().getTime())
                    .map(OptionTokenResult::getBaseTokenOption)
                    .collect(Collectors.toList());
        } else {
            return tokenResults
                    .stream()
                    .filter(option -> option.getBaseTokenOption()
                            .getExpiration() > new Date().getTime())
                    .map(OptionTokenResult::getBaseTokenOption)
                    .collect(Collectors.toList());
        }
    }

    public List<TokenFuturesResult> getFuturesTokens(Header header, boolean expired) {
        List<SymbolResult> symbolResults = orgFuturesSymbolMap.getOrDefault(header.getOrgId(), Lists.newArrayList());
        if (symbolResults.isEmpty()) {
            return Lists.newArrayList();
        }
        return symbolResults.stream().map(FuturesTools::toFuturesTokenResult).collect(Collectors.toList());
    }

    public List<ContractsResult> getContracts(Header header, boolean expired) {
        List<SymbolResult> symbolResults = orgFuturesSymbolMap.getOrDefault(header.getOrgId(), Lists.newArrayList());
        if (symbolResults.isEmpty()) {
            return Lists.newArrayList();
        }
        return symbolResults.stream()
                .filter(symbolResult -> !symbolResult.getHideFromOpenapi())
                .map(symbolResult -> {
                    String key = String.format("%s_%s", symbolResult.getOrgId(), symbolResult.getSymbolId());
                    if (expired) {
                        ContractsResult result = getContract(symbolResult);
                        contractResultCache.put(key, result);
                        return result;
                    } else {
                        try {
                            return contractResultCache.get(key, () -> {
                                return getContract(symbolResult);
                            });
                        } catch (ExecutionException e) {
                            return getContract(symbolResult);
                        }
                    }
                }).collect(Collectors.toList());
    }

    protected ContractsResult getContract(SymbolResult symbolResult) {
        try {
            Long orgId = symbolResult.getOrgId();
            Long exchangeId = symbolResult.getExchangeId();
            Depth depth = getDepth(exchangeId, symbolResult.getSymbolId(), orgId, 100);
            GetQuoteRequest request = GetQuoteRequest.newBuilder()
                    .setBaseRequest(BaseRequest.newBuilder().setOrganizationId(orgId).build())
                    .setExchangeId(exchangeId)
                    .setSymbol(symbolResult.getSymbolId())
                    .build();
            GetRealtimeReply realtimeReply = grpcQuoteService.getRealtime(request, orgId);
            Realtime realtime = null;
            if (realtimeReply.getRealtimeCount() > 0) {
                realtime = realtimeReply.getRealtime(0);
            }

            Map<String, String> indexs = indexCaches.get(orgId, () -> {
                GetIndicesReply indicesReply = grpcQuoteService.getIndices(GetIndicesRequest.newBuilder()
                        .setBaseRequest(BaseRequest.newBuilder().setOrganizationId(orgId).build())
                        .addAllSymbols(orgFuturesSymbolMap.get(orgId).stream().map(item -> {
                            return item.getTokenFutures().getDisplayIndexToken();
                        }).distinct().collect(Collectors.toList())).build(), orgId);
                Map<String, String> map = Maps.newHashMap();
                indicesReply.getIndicesMapMap().entrySet().stream().forEach(stringIndexEntry -> {
                    map.put(stringIndexEntry.getKey(), DecimalUtil.toTrimString(stringIndexEntry.getValue().getIndex()));
                });
                return map;
            });

            String index = indexs.getOrDefault(symbolResult.getTokenFutures().getDisplayIndexToken(), "0");
            Map<String, FundingRate> rates = fundingRateCaches.get(orgId, () -> {
                GetFundingRatesResponse response = grpcFuturesOrderService.getFundingRates(GetFundingRatesRequest.newBuilder()
                        .setHeader(io.bhex.broker.grpc.common.Header.newBuilder().setOrgId(orgId).build())
                        .build());
                Map<String, FundingRate> map = Maps.newHashMap();
                response.getFundingInfoList().stream().forEach(fundingRate -> {
                    map.put(fundingRate.getTokenId(), fundingRate);
                });
                return map;
            });
            FundingRate fundingRate = rates.get(symbolResult.getSymbolId());
            String openInterest = "0";
            Map<String, String> openInterests = openInterestCaches.get(exchangeId, () -> {
                GetTotalPositionRequest request1 = GetTotalPositionRequest.newBuilder()
                        .setExchangeId(exchangeId)
                        .setBaseRequest(BaseRequest.newBuilder().setOrganizationId(orgId).build())
                        .build();
                GetTotalPositionReply reply = grpcFuturesBHServerService.getTotalPosition(request1, orgId);
                return reply.getTokenPositionMap();
            });
            if (openInterests != null && openInterests.containsKey(symbolResult.getSymbolId())) {
                openInterest = new BigDecimal(openInterests.get(symbolResult.getSymbolId())).setScale(0, BigDecimal.ROUND_DOWN).toEngineeringString();
            }
            return ContractsResult.builder()
                    .symbol(symbolResult.getSymbolId())
                    .symbolName(symbolResult.getSymbolName())
                    .baseToken(symbolResult.getBaseTokenName())
                    .quoteToken(symbolResult.getQuoteTokenName())
                    .high(realtime == null ? BigDecimal.ZERO : new BigDecimal(realtime.getH()))
                    .low(realtime == null ? BigDecimal.ZERO : new BigDecimal(realtime.getL()))
                    .baseVolume(realtime == null ? BigDecimal.ZERO : new BigDecimal(realtime.getV()))
                    .quoteVolume(realtime == null ? BigDecimal.ZERO : new BigDecimal(realtime.getQv()))
                    .lastPrice(realtime == null ? BigDecimal.ZERO : new BigDecimal(realtime.getC()))
                    .bid(depth == null || depth.getBids().getBookOrderCount() == 0 ? BigDecimal.ZERO : DecimalUtil.toBigDecimal(depth.getBids().getBookOrder(0).getPrice()))
                    .ask(depth == null || depth.getAsks().getBookOrderCount() == 0 ? BigDecimal.ZERO : DecimalUtil.toBigDecimal(depth.getAsks().getBookOrder(0).getPrice()))
                    .productType("futures")
                    .index(symbolResult.getTokenFutures().getDisplayIndexToken())
                    .indexPrice(index == null ? BigDecimal.ZERO : new BigDecimal(index))
                    .indexBaseToken(symbolResult.getTokenFutures().getCurrency())
                    .startTs(realtime == null ? 0 : Long.parseLong(realtime.getT()) / 1000 - 86400)
                    .endTs(realtime == null ? 0 : Long.parseLong(realtime.getT()) / 1000)
                    .fundingRate(fundingRate == null ? BigDecimal.ZERO : new BigDecimal(fundingRate.getSettleRate()))
                    .nextFundingRateTs(fundingRate == null ? 0 : (int) (fundingRate.getNextSettleTime() / 1000))
                    .nextFundingRate(fundingRate == null ? BigDecimal.ZERO : new BigDecimal(fundingRate.getFundingRate()))
                    .openInterest(openInterest)
                    .build();
        } catch (Exception e) {
            log.error(String.format("getContract,orgId:%s,symbol:%s", symbolResult.getOrgId(), symbolResult.getSymbolId()), e);
            return ContractsResult.builder()
                    .symbol(symbolResult.getSymbolName())
                    .baseToken(symbolResult.getBaseTokenName())
                    .quoteToken(symbolResult.getQuoteTokenName())
                    .productType("futures")
                    .build();
        }

    }

    public Depth getDepth(Long exchangeId, String symbol, Long orgId, int limitCount) {
        int dumpScale = 18;
        GetQuoteRequest request = GetQuoteRequest.newBuilder()
                .setBaseRequest(BaseRequest.newBuilder().setOrganizationId(orgId).build())
                .setExchangeId(exchangeId)
                .setSymbol(symbol)
                .setDumpScale(dumpScale)
                .setLimitCount(limitCount)
                .build();
        GetDepthReply reply = grpcQuoteService.getPartialDepth(request, orgId);
        if (reply == null || CollectionUtils.isEmpty(reply.getDepthList())) {
            log.warn("getDepth is null. exchangeId:{}, symbol:{}", exchangeId, symbol);
            return null;
        }
        Depth depth = reply.getDepthList().get(0);
        return depth;
    }

    public SymbolResult getFuturesSymbolResult(long orgId, String tokenId, String symbolId) {
        Optional<SymbolResult> symbolResult = orgFuturesSymbolMap.getOrDefault(orgId, Lists.newArrayList())
                .stream()
                .filter(t -> t.getBaseTokenId().equalsIgnoreCase(tokenId)
                        || t.getSymbolId().equalsIgnoreCase(symbolId))
                .findAny();
        return symbolResult.orElse(null);
    }

    private OptionTokenResult getOptionTokenResult(Token token) {
        OptionTokenResult.Builder builder = OptionTokenResult.builder()
                .orgId(token.getOrgId())
                .tokenId(token.getTokenId())
                .tokenName(token.getTokenName())
                .tokenFullName(token.getTokenFullName())
                .iconUrl(token.getIconUrl())
                .allowDeposit(token.getAllowDeposit())
                .allowWithdraw(token.getAllowWithdraw())
                .baseTokenSymbols(token.getBaseTokenSymbolsList().stream()
                        .filter(symbol -> symbol.getOrgId() == token.getOrgId())
                        .map(this::getSymbolBase).sorted(Comparator.comparing(SymbolResult::getSymbolId))
                        .collect(Collectors.toList()))
                .quoteTokenSymbols(token.getQuoteTokenSymbolsList().stream()
                        .filter(symbol -> symbol.getOrgId() == token.getOrgId())
                        .map(this::getSymbolBase).sorted(Comparator.comparing(SymbolResult::getSymbolId))
                        .collect(Collectors.toList()))
                .isEOS(token.getIsEos())
                .tokenType(token.getTokenType())
                .needKycQuantity(token.getNeedKycQuantity())
                .needAddressTag(token.getNeedAddressTag())
                .exchangeId(token.getExchangeId());
        setTokenOptionInfo(token, builder);
        return builder.build();
    }

    private void setTokenOptionInfo(Token token, OptionTokenResult.Builder builder) {
        TokenOptionInfo tokenOptionInfo = token.getTokenOptionInfo();
        if (tokenOptionInfo != null && StringUtils.isNotEmpty(tokenOptionInfo.getTokenId())) {
            TokenOptionInfo optionInfo = token.getTokenOptionInfo();
            TokenOptionResult optionResult = TokenOptionResult.builder()
                    .symbol(optionInfo.getTokenId())
                    .strike(optionInfo.getStrikePrice())
                    .created(optionInfo.getIssueDate())
                    .expiration(optionInfo.getSettlementDate())
                    .optionType(optionInfo.getIsCall())
                    .maxPayOff(optionInfo.getMaxPayOff())
                    .underlying(optionInfo.getIndexToken())
                    .settlement("weekly")
                    .build();
            builder.baseTokenOption(optionResult);
        }
    }

    private SymbolResult getSymbolBase(SymbolBaseInfo symbolBaseInfo) {
        return SymbolResult.builder()
                .orgId(symbolBaseInfo.getOrgId())
                .exchangeId(symbolBaseInfo.getExchangeId())
                .symbolId(symbolBaseInfo.getSymbolId())
                .symbolName(symbolBaseInfo.getSymbolName())
                .baseTokenId(symbolBaseInfo.getBaseTokenId())
                .baseTokenName(symbolBaseInfo.getBaseTokenName())
                .quoteTokenId(symbolBaseInfo.getQuoteTokenId())
                .quoteTokenName(symbolBaseInfo.getQuoteTokenName())
                .build();
    }

    private void initFuturesSymbol(io.bhex.broker.grpc.common.Header header) {
        try {
            List<Symbol> symbolList = Lists.newLinkedList();
            Set<String> symbolKeys = Sets.newHashSet();
            for (Long orgId : orgIdBrokerInfoMap.keySet()) {
                grpcBasicService.queryFuturesSymbols(io.bhex.broker.grpc.common.Header.newBuilder().setOrgId(orgId).build()).forEach(symbol -> {
                    String key = symbol.getOrgId() + "#" + symbol.getExchangeId() + "#" + symbol.getSymbolId();
                    if (!symbolKeys.contains(key)) {
                        symbolList.add(symbol);
                        symbolKeys.add(key);
                    }
                });
            }
            if (CollectionUtils.isEmpty(symbolList)) {
                log.warn("initFuturesSymbol error. orgId:{}", header.getOrgId());
                return;
            }
            List<SymbolResult> symbolResultList = symbolList.stream().map(this::getSymbolResult).collect(Collectors.toList());
            Map<Long, List<SymbolResult>> orgSymbolMap = symbolResultList
                    .stream()
                    .collect(Collectors.groupingBy(SymbolResult::getOrgId));

            BasicService.orgFuturesSymbolMap = ImmutableMap.copyOf(orgSymbolMap);

            Map<Long, Long> exchangeIdToOrgId = Maps.newHashMap();
            symbolList.stream().forEach(symbol -> {
                exchangeIdToOrgId.put(symbol.getExchangeId(), symbol.getOrgId());
            });
            BasicService.exchangeIdMaps = ImmutableMap.copyOf(exchangeIdToOrgId);
        } catch (Exception e) {
            log.error("initFuturesSymbol error", e);
        }
    }

    private void initAccountWhiteLists() {
        try {
            Map<String, List<Long>> accountWhiteListMap = new HashMap<>();

            List<Long> futuresAccountWhiteList = grpcBasicService.getWhiteList(FUTURES_ACCOUNT_WHITELIST_KEY);
            accountWhiteListMap.put(FUTURES_ACCOUNT_WHITELIST_KEY, futuresAccountWhiteList);

            List<Long> coinAccountWhiteList = grpcBasicService.getWhiteList(COIN_ACCOUNT_WHITELIST_KEY);
            accountWhiteListMap.put(COIN_ACCOUNT_WHITELIST_KEY, coinAccountWhiteList);

            log.warn("initAccountWhiteLists: {}", accountWhiteListMap);
            BasicService.accountWhiteListMap = ImmutableMap.copyOf(accountWhiteListMap);
        } catch (Exception e) {
            log.error("initAccountWhiteLists error", e);
        }
    }

    private void initUserWhiteLists() {
        try {
            Map<String, List<Long>> userWhiteListMapTmp = new HashMap<>();

            List<Long> futuresUserWhiteList = grpcBasicService.getWhiteList(FUTURES_USER_WHITELIST_KEY);
            userWhiteListMapTmp.put(FUTURES_USER_WHITELIST_KEY, futuresUserWhiteList);

            List<Long> coinUserWhiteList = grpcBasicService.getWhiteList(COIN_USER_WHITELIST_KEY);
            userWhiteListMapTmp.put(COIN_USER_WHITELIST_KEY, coinUserWhiteList);

            log.warn("initUserWhiteLists: {}", userWhiteListMapTmp);
            BasicService.userWhiteListMap = ImmutableMap.copyOf(userWhiteListMapTmp);
        } catch (Exception e) {
            log.error("initUserWhiteLists error", e);
        }
    }

    /**
     * 判断账户是否在白名单中（包括币币白名单和期货白名单）
     */
    public boolean isAccountWhiteList(Long accountId) {
        List<Long> futuresAccountWhiteList = BasicService.accountWhiteListMap.getOrDefault(
                FUTURES_ACCOUNT_WHITELIST_KEY, Lists.newArrayList());

        List<Long> coinAccountWhiteList = BasicService.accountWhiteListMap.getOrDefault(
                COIN_ACCOUNT_WHITELIST_KEY, Lists.newArrayList());
        return futuresAccountWhiteList.contains(accountId) || coinAccountWhiteList.contains(accountId);
    }

    /**
     * 判断用户是否在币币白名单中
     */
    public boolean isUserInCoinWhiteList(Long userId) {
        List<Long> coinUserWhiteList = BasicService.userWhiteListMap.getOrDefault(
                COIN_USER_WHITELIST_KEY, Lists.newArrayList());
        return coinUserWhiteList.contains(userId);
    }

    /**
     * 判断用户是否在期货白名单中
     */
    public boolean isUserInFuturesWhiteList(Long userId) {
        List<Long> futuresUserWhiteList = BasicService.userWhiteListMap.getOrDefault(
                FUTURES_USER_WHITELIST_KEY, Lists.newArrayList());
        return futuresUserWhiteList.contains(userId);
    }

    public void initMarginConfig() {
        try {
            //获取margin Token
            Map<Long, List<TokenConfig>> tokenMap = new HashMap<>();
            GetTokenConfigRequest tokenConfigRequest = GetTokenConfigRequest.newBuilder()
                    .setHeader(io.bhex.broker.grpc.common.Header.newBuilder().setOrgId(0L).build())
                    .build();
            GetTokenConfigResponse tokenConfigResponse = grpcMarginService.getTokenConfig(tokenConfigRequest);
            tokenMap = tokenConfigResponse.getTokenConfigList().stream()
                    .collect(Collectors.groupingBy(TokenConfig::getOrgId));
            Set<Long> orgIds = tokenMap.keySet();
            //获取margin risk
            Map<Long, RiskConfig> riskConfigMap = new HashMap<>();
            for (Long orgId : orgIds) {
                GetRiskConfigRequest riskConfigRequest = GetRiskConfigRequest.newBuilder()
                        .setHeader(io.bhex.broker.grpc.common.Header.newBuilder().setOrgId(orgId).build())
                        .build();
                GetRiskConfigResponse riskConfigResponse = grpcMarginService.getRiskConfigResponse(riskConfigRequest);
                if (riskConfigResponse.getRiskConfigList().isEmpty()) {
                    continue;
                }
                riskConfigMap.put(orgId, riskConfigResponse.getRiskConfig(0));
            }
            //获取interest 配置
            Map<Long, List<InterestConfig>> interestConfigMap = new HashMap<>();
            GetInterestConfigRequest interestConfigRequest = GetInterestConfigRequest.newBuilder()
                    .setHeader(io.bhex.broker.grpc.common.Header.newBuilder().setOrgId(0L).build())
                    .build();
            GetInterestConfigResponse interestConfigResponse = grpcMarginService.getInterestConfig(interestConfigRequest);
            interestConfigMap = interestConfigResponse.getInterestConfigList().stream()
                    .collect(Collectors.groupingBy(InterestConfig::getOrgId));

            orgMarginTokenMap = ImmutableMap.copyOf(tokenMap);
            orgMarginRiskMap = ImmutableMap.copyOf(riskConfigMap);
            orgMarginInterestMap = ImmutableMap.copyOf(interestConfigMap);

        } catch (Exception e) {
            log.error("initMarginConfig error", e);
        }
    }

    public List<TokenConfig> getMarginTokenConfig(Long orgId) {
        return orgMarginTokenMap.getOrDefault(orgId, new ArrayList<>());
    }

    public List<InterestConfig> getMarginInterestConfig(Long orgId) {
        return orgMarginInterestMap.getOrDefault(orgId, new ArrayList<>());
    }

    public RiskConfig getMarginRisk(Long orgId) {
        return orgMarginRiskMap.get(orgId);
    }

    public List<EtfPriceResult> getEtfPrices() {
        GetEtfSymbolPriceRequest request = GetEtfSymbolPriceRequest.newBuilder()
                .setHeader(io.bhex.broker.grpc.common.Header.newBuilder().setOrgId(6002).build()) //此处的orgId没有实际用途
                .build();
        List<GetEtfSymbolPriceResponse.EtfPrice> list = grpcBasicService.getEtfSymbolPrice(request);
        List<EtfPriceResult> results = list.stream()
                .map(e -> {
                    EtfPriceResult r = EtfPriceResult.builder().build();
                    r.setUnderlyingIndexId(e.getUnderlyingIndexId());
                    r.setContractSymbolId(e.getContractSymbolId());
                    r.setIsLong(e.getIsLong());
                    r.setLeverage(e.getLeverage());
                    r.setSymbolId(e.getSymbolId());
                    r.setEtfPrice(e.getEtfPrice());
                    r.setUnderlyingPrice(e.getUnderlyingPrice());
                    r.setTime(e.getTime());
                    return r;
                })
                .collect(Collectors.toList());
        return results;
    }

}
