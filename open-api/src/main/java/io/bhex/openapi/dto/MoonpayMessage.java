package io.bhex.openapi.dto;

import lombok.Data;

@Data
public class MoonpayMessage {
    MoonpayTransaction data;
    String type;
    String externalCustomerId;
}