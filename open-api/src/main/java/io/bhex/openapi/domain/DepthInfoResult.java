package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DepthInfoResult {

    private Long time;
    private Integer level;
    private Long exchangeId;
    private List<DepthInfo> depthInfoList;

    @Data
    @Builder
    public static class DepthInfo {
        private String symbolId;
        private List<OrderInfoList> bid;
        private List<OrderInfoList> ask;
    }

    @Data
    @Builder
    public static class OrderInfoList {
        private String price;
        private String originalPrice;
        private List<OrderInfo> orderInfo;
    }

    @Data
    @Builder
    public static class OrderInfo {
        private String quantity;
        private Long accountId;
    }

}
