/*
 ************************************
 * @项目名称: bh
 * @文件名称: RequestUtil
 * @Date 2018/09/09
 * @Author will.zhao@bhex.io
 * @Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 **************************************
 */
package io.bhex.openapi.util;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.ListenableFuture;
import io.bhex.base.account.NewOrderReply;
import io.bhex.base.account.NewOrderRequest;
import io.bhex.base.account.OrderServiceGrpc;
import io.bhex.base.constants.ProtoConstants;
import io.bhex.base.exception.ErrorStatusRuntimeException;
import io.bhex.base.proto.*;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RequestUtil {

    static AtomicLong clientIDGen = new AtomicLong(System.currentTimeMillis());

    public static NewOrderRequest getNewOrderRequest(long accountId, String symbol) {
        BigDecimal quantity = BigDecimal.valueOf(0.01);
        BigDecimal price = BigDecimal.valueOf(100);

        return createNewOrderRequest(accountId, symbol,
                OrderTypeEnum.LIMIT, OrderSideEnum.BUY, quantity, price);
    }

    private static NewOrderRequest createNewOrderRequest(long accountId, String symbol,
                                                         OrderTypeEnum orderType, OrderSideEnum orderSide,
                                                         BigDecimal quantity, BigDecimal price) {
        BigDecimal orderQuantity = quantity;
        BigDecimal orderPrice = price;
        BigDecimal orderAmount = orderQuantity.multiply(orderPrice)
                .setScale(ProtoConstants.PRECISION, ProtoConstants.ROUNDMODE);

        return NewOrderRequest.newBuilder()
                .setSymbolId(symbol)
                .setOrderType(orderType)
                .setSide(orderSide)
                .setQuantity(DecimalUtil.fromBigDecimal(quantity))
                .setPrice(DecimalUtil.fromBigDecimal(price))
                .setAmount(DecimalUtil.fromBigDecimal(orderAmount))  // = price* quantity
                .setTimeInForce(OrderTimeInForceEnum.GTC)
                .setAccountId(accountId)
                .setExchangeId(301)
                .setOrgId(6001)
                .setClientOrderId(String.valueOf(clientIDGen.getAndIncrement()))
                .build();

    }

    public static NewOrderReply createOrder(boolean isAsync, Channel channel, long accountId, String symbol) {
        final OrderServiceGrpc.OrderServiceBlockingStub blockStub = OrderServiceGrpc.newBlockingStub(channel);
        final OrderServiceGrpc.OrderServiceFutureStub futureStub = OrderServiceGrpc.newFutureStub(channel);

        NewOrderReply reply = null;
        NewOrderRequest request = getNewOrderRequest(accountId, symbol);

        if (isAsync) {

            ListenableFuture<NewOrderReply> future = futureStub.newOrder(request);
            try {
                reply = future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("async invoke occured error {}", Throwables.getStackTraceAsString(e));
            }

        } else {
            try {
                reply = blockStub.newOrder(request);
                if (reply.getStatus() == OrderStatusEnum.REJECTED) {
                    log.error("newOrder: REJECTED (request: {})", request);
                }
            } catch (StatusRuntimeException e) {
                ErrorStatus errorStatus = e.getTrailers().get(ErrorStatusRuntimeException.ERROR_STATUS_KEY);
                if (errorStatus != null) {
                    ErrorCode error = errorStatus.getCode();
                    log.warn("newOrder failed: {} (request: {})", error.name(), request);
                }
                log.error("{}", Throwables.getStackTraceAsString(e));
            }

        }
        return reply;
    }

    public static String getRemoteIp(HttpServletRequest request) {
        String ip;
        ip = request.getHeader("X-Forwarded-For");
        log.info("GetIp: X-Forwarded-For:{}", ip);
        if (!Strings.isNullOrEmpty(ip) && ip.contains(",")) {
            ip = ip.split(",")[0];
            log.info("GetIp: X-Forwarded-For[0]:{}", ip);
        }
        if (!Strings.isNullOrEmpty(ip) && InetAddresses.isInetAddress(ip)) {
            return ip;
        }
        ip = request.getHeader("X-Real-IP");
        log.info("GetIp: X-Real-IP:{}", ip);
        if (!Strings.isNullOrEmpty(ip) && InetAddresses.isInetAddress(ip)) {
            return ip;
        }
        log.info("GetIp: RemoteAddr:{}", request.getRemoteAddr());
        return request.getRemoteAddr();
    }

}
