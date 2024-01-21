/*
 ************************************
 * @项目名称: static-parent
 * @文件名称: TokenOptionResult
 * @Date 2019/01/14
 * @Author will.zhao@bhex.io
 * @Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 * 注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 **************************************
 */
package io.bhex.openapi.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder", toBuilder = true)
public class TokenOptionResult {

    private String symbol;//期权名称

    private String strike;//行权价

    private Long created;//生效/上线日期

    private Long expiration;//到期/交割时间

    private int optionType;//call or put

    private String maxPayOff;//最大赔付

    private String underlying; //标的指数

    private String settlement;

}