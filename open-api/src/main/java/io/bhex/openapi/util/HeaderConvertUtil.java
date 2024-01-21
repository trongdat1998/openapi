/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.util
 *@Date 2018/8/22
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
package io.bhex.openapi.util;

import com.google.common.base.Strings;
import io.bhex.broker.grpc.common.Header;
import io.bhex.broker.grpc.common.Platform;

public class HeaderConvertUtil {

    public static Header convertHeader(io.bhex.broker.common.entity.Header header) {
        Header.Builder headerBuilder = Header.newBuilder()
                .setOrgId(header.getOrgId())
                .setUserId(header.getUserId() == null ? 0L : header.getUserId())
                .setUserAgent(Strings.nullToEmpty(header.getUserAgent()))
                .setReferer(Strings.nullToEmpty(header.getReferer()))
                .setRemoteIp(Strings.nullToEmpty(header.getRemoteIp()))
                .setPlatform(Platform.OPENAPI)
                .setLanguage(Strings.nullToEmpty(header.getLanguage()))
                .setRequestId(Strings.nullToEmpty(header.getRequestId()))
                .setRequestUri(Strings.nullToEmpty(header.getServerUri()))
                .setChannel(Strings.nullToEmpty(header.getChannel()))
                .setSource(Strings.nullToEmpty(header.getSource()))
                .setServerName(Strings.nullToEmpty(header.getServerName()))
                .setRequestTime(header.getRequestTime());
        if (header.getAppBaseHeader() != null) {
            io.bhex.broker.grpc.common.AppBaseHeader baseHeader = io.bhex.broker.grpc.common.AppBaseHeader.newBuilder()
                    .setApp(Strings.nullToEmpty(header.getAppBaseHeader().getApp()))
                    .setAppId(Strings.nullToEmpty(header.getAppBaseHeader().getAppId()))
                    .setAppVersion(Strings.nullToEmpty(header.getAppBaseHeader().getAppVersion()))
                    .setNett(Strings.nullToEmpty(header.getAppBaseHeader().getNett()))
                    .setChannel(Strings.nullToEmpty(header.getAppBaseHeader().getChannel()))
                    .setOsType(Strings.nullToEmpty(header.getAppBaseHeader().getOsType()))
                    .setOsVersion(Strings.nullToEmpty(header.getAppBaseHeader().getOsVersion()))
                    .setImei(Strings.nullToEmpty(header.getAppBaseHeader().getImei()))
                    .setImsi(Strings.nullToEmpty(header.getAppBaseHeader().getImsi()))
                    .setChannel(Strings.nullToEmpty(header.getChannel()))
                    .build();
            headerBuilder.setAppBaseHeader(baseHeader);
        }
        return headerBuilder.build();
    }

}
