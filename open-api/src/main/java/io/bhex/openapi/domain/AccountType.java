package io.bhex.openapi.domain;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.broker.common.exception.BrokerException;
import io.bhex.broker.grpc.common.AccountTypeEnum;

/**********************************
 *@项目名称: broker-parent
 *@文件名称: io.bhex.broker.domain
 *@Date 2018/6/10
 *@Author peiwei.ren@bhex.io 
 *@Copyright（C）: 2018 BlueHelix Inc.   All rights reserved.
 *注意：本内容仅限于内部传阅，禁止外泄以及用于其他的商业目的。
 ***************************************/
public enum AccountType {
    UNKNOWN(0, ""),
    COMMON(-1, "coin + option + futures + margin"), // 主账户
    MAIN(1, "main"), // 币币子账户
    OPTION(2, "option"), // 期权子账户，目前是不存在的
    FUTURES(3, "futures"),
    MARGIN(27, "margin"),; // 期货子账户

    private final int value;
    private final String type;

    AccountType(int value, String type) {
        this.value = value;
        this.type = type;
    }

    public static AccountType fromValue(int value) {
        for (AccountType accountType : AccountType.values()) {
            if (accountType.value == value) {
                return accountType;
            }
        }
        return UNKNOWN;
    }

    public static AccountType fromAccountTypeEnum(AccountTypeEnum accountTypeEnum) {
        switch (accountTypeEnum) {
            case COIN:
                return MAIN;
            case OPTION:
                return OPTION;
            case FUTURE:
                return FUTURES;
            case MARGIN:
                return MARGIN;
            default:
                throw new BrokerException(BrokerErrorCode.PARAM_INVALID);
        }
    }

    public static AccountTypeEnum toAccountTypeEnum(int value) {
        if (value == 1) {
            return AccountTypeEnum.COIN;
        } else if (value == 2) {
            return AccountTypeEnum.OPTION;
        } else if (value == 3) {
            return AccountTypeEnum.FUTURE;
        }else if (value == 27){
            return AccountTypeEnum.MARGIN;
        }
        throw new BrokerException(BrokerErrorCode.PARAM_INVALID);
    }

    public int value() {
        return this.value;
    }

    public String type() {
        return this.type;
    }
}
