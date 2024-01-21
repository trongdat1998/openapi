package io.bhex.openapi.controller;

import com.mysql.cj.util.StringUtils;
import io.bhex.base.proto.DecimalUtil;
import io.bhex.broker.common.entity.Header;
import io.bhex.broker.common.util.JsonUtil;
import io.bhex.openapi.dto.MoonpayMessage;
import io.bhex.openapi.dto.MoonpayTransaction;
import io.bhex.openapi.service.OtcThirdPartyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping(value = {"/openapi/third", "/openapi/v1/third"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class OtcThirdPartyController {

    @Resource
    private OtcThirdPartyService otcThirdPartyService;

    //测试key
    //private final String WEB_HOOK_KEY = "wk_test_VmdmKmVk6MLBNOyU1pkF4SJZRab71YU";
    //真实key
    private final String WEB_HOOK_KEY = "wk_live_CJk53ZPe1roabDVVDppcVC9TpfafpvEG";


    private final String TIME_STAMP_KEY = "t=";
    private final String SIGNATURE_KEY = ",s=";

    @RequestMapping(value = {"/order"})
    @ResponseBody
    public void moonpayTransaction(Header header, HttpServletRequest request, @RequestBody String body) {
        //判断消息体不为空
        if (StringUtils.isNullOrEmpty(body)) {
            log.info("moonpayTransaction message body is null or empty.");
            return;
        }
        //判断请求头里包含签名值
        String signature = request.getHeader("Moonpay-Signature-V2").toLowerCase();
        if (StringUtils.isNullOrEmpty(signature)) {
            log.info("moonpayTransaction header signature is null or empty.");
            return;
        }
        //解析字符串
        String messageTime = signature.substring(signature.indexOf(TIME_STAMP_KEY) + TIME_STAMP_KEY.length(), signature.indexOf(SIGNATURE_KEY));
        String messageSign = signature.substring(signature.indexOf(SIGNATURE_KEY) + SIGNATURE_KEY.length());
        String data = messageTime + "." + body;
        String dataSign = doSignature(data, WEB_HOOK_KEY);
        //校验签名，不一致则不继续处理消息
        if (!dataSign.equalsIgnoreCase(messageSign)) {
            log.info("moonpayTransaction sign error: dataSign:{},messageSign:{}. ", dataSign, messageSign);
            return;
        }
        //解析主推消息体
        MoonpayMessage message = JsonUtil.defaultGson().fromJson(body, MoonpayMessage.class);
        if (message == null || message.getData() == null) {
            log.info("moonpayTransaction message or data is null.");
            return;
        }
        MoonpayTransaction transaction = message.getData();
        log.info("moonpayTransaction transactionId:{}, status:{}, externalTransactionId:{}.",
                transaction.getId(), transaction.getStatus(), transaction.getExternalTransactionId());
        if (StringUtils.isNullOrEmpty(transaction.getId()) || StringUtils.isNullOrEmpty(transaction.getStatus()) ||
                StringUtils.isNullOrEmpty(transaction.getExternalTransactionId())) {
            return;
        }
        // 解析费用相关字段值
        String feeAmount = "";
        if (transaction.getFeeAmount() != null && transaction.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            feeAmount = DecimalUtil.toTrimString(transaction.getFeeAmount());
        }
        String extraFeeAmount = "";
        if (transaction.getExtraFeeAmount() != null && transaction.getExtraFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            extraFeeAmount = DecimalUtil.toTrimString(transaction.getExtraFeeAmount());
        }
        String networkFeeAmount = "";
        if (transaction.getNetworkFeeAmount() != null && transaction.getNetworkFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            networkFeeAmount = DecimalUtil.toTrimString(transaction.getNetworkFeeAmount());
        }
        otcThirdPartyService.moonpayTransaction(header, transaction.getId(), transaction.getStatus(),
                transaction.getExternalTransactionId(), feeAmount, extraFeeAmount, networkFeeAmount);
    }

    private String doSignature(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
            String sign = new String(Hex.encodeHex(sha256_HMAC.doFinal(data.getBytes())));
            return sign;
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign message.", e);
        }
    }

}
