package io.bhex.openapi.constant;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ApiErrorCode {

    SUCCESS(0, "success"),

    UNKNOWN(-1000, "An unknown error occured while processing the request."),
    DISCONNECTED(-1001, "Internal error."),
    UNAUTHORIZED(-1002, "You are not authorized to execute this request"),
    TOO_MANY_REQUESTS(-1003, "Too many requests, current limit is %s requests per %s ."),
    BAD_REQUEST(-1004, "Bad request"),
    NO_PERMISSION(-1005, "No Permission"),
    UNEXPECTED_RESP(-1006, "Execution status unknown."),
    TIMEOUT(-1007, "Timeout waiting for response from backend server."),
    UNKNOWN_ORDER_COMPOSITION(-1014, "Unsupported order combination."),
    TOO_MANY_ORDERS(-1015, "Too many new orders; current limit is %s orders per %s."),
    SERVICE_SHUTTING_DOWN(-1016, "This service is no longer available."),
    UNSUPPORTED_OPERATION(-1020, "This operation is not supported."),
    INVALID_TIMESTAMP(-1021, "Timestamp for this request is outside of the recvWindow."),
    INVALID_SIGNATURE(-1022, "Signature for this request is not valid."),
    BIND_IP_WHITE_LIST_FIRST(-1023, "Please set IP whitelist before using API"),

    ERR_UPSTREAM_BUSINESS(-1030, "Business error."),
    FEATURE_SUSPENDED(-1031, "The feature has been suspended"),

    ILLEGAL_CHARS(-1100, "Illegal characters found in parameter '%s'; legal range is '%s'."),
    TOO_MANY_PARAMETERS(-1101, "Too many parameters sent for this endpoint."),
    MANDATORY_PARAM_EMPTY_OR_MALFORMED(-1102, "Mandatory parameter '%s' was not sent, was empty/null, or malformed."),
    UNKNOWN_PARAM(-1103, "An unknown parameter was sent."),
    UNREAD_PARAMETERS(-1104, "Not all sent parameters were read."),
    PARAM_EMPTY(-1105, "Parameter '%s' was empty."),
    PARAM_NOT_REQUIRED(-1106, "Parameter '%s' sent when not required."),
    BAD_PRECISION(-1111, "Precision is over the maximum defined for this asset."),
    NO_DEPTH(-1112, "No orders on book for symbol."),
    TIF_NOT_REQUIRED(-1114, "TimeInForce parameter sent when not required."),
    INVALID_TIF(-1115, "Invalid timeInForce."),
    INVALID_ORDER_TYPE(-1116, "Invalid orderType."),
    INVALID_SIDE(-1117, "Invalid side."),
    EMPTY_NEW_CL_ORDID(-1118, "New client order ID was empty."),
    EMPTY_ORG_CL_ORDID(-1119, "Original client order ID was empty."),
    BAD_INTERVAL(-1120, "Invalid interval."),
    BAD_SYMBOL(-1121, "Invalid symbol."),
    INVALID_LISTEN_KEY(-1125, "This listenKey does not exist."),
    MORE_THAN_XX_HOURS(-1127, "Lookup interval is too big."),
    OPTIONAL_PARAMS_BAD_COMBO(-1128, "Combination of optional parameters invalid."),
    INVALID_PARAMETER(-1130, "Data sent for paramter '%s' is not valid."),
    INSUFFICIENT_BALANCE(-1131, "Balance insufficient "),

    ORDER_PRICE_TOO_HIGH(-1132, "Order price too high."),
    ORDER_PRICE_TOO_SMALL(-1133, "Order price lower than the minimum."),
    ORDER_PRICE_PRECISION_TOO_LONG(-1134, "Order price decimal too long."),
    ORDER_QUANTITY_TOO_BIG(-1135, "Order quantity too large."),
    ORDER_QUANTITY_TOO_SMALL(-1136, "Order quantity lower than the minimum."),
    ORDER_QUANTITY_PRECISION_TOO_LONG(-1137, "Order volume decimal too long"),
    ORDER_PRICE_WAVE_EXCEED(-1138, "Order price exceeds permissible range."),
    ORDER_HAS_FILLED(-1139, "Order has been filled."),
    ORDER_AMOUNT_TOO_SMALL(-1140, "Transaction amount lower than the minimum."),
    DUPLICATED_ORDER(-1141, "Duplicate clientOrderId"),
    ORDER_CANCELLED(-1142, "Order has been canceled"),
    ORDER_NOT_FOUND_ON_ORDER_BOOK(-1143, "Cannot be found on order book"),
    ORDER_LOCKED(-1144, "Order has been locked"),
    UNSUPPORTED_ORDER_TYPE_UNSUPPORTED_CANCEL(-1145, "This order type does not support cancellation"),
    CREATE_ORDER_TIMEOUT(-1146, "Order creation timeout"),
    CANCEL_ORDER_TIMEOUT(-1147, "Order cancellation timeout"),
    ORDER_AMOUNT_PRECISION_TOO_LONG(-1148, "Market order amount decimal too long"),
    CREATE_ORDER_FAILED(-1149, "Create order failed"),
    CANCEL_ORDER_FAILED(-1150, "Cancel order failed"),
    SYMBOL_PROHIBIT_ORDER(-1151, "The trading pair is not open yet"),
    COMING_SOON(-1152, "Coming soon"),
    USER_NOT_EXIST(-1153, "User not exist"),

    INVALID_PRICE_TYPE(-1154, "Invalid price type"),
    INVALID_POSITION_SIDE(-1155, "Invalid position side"),
    ORDER_QUANTITY_INVALID(-1156, "Order quantity invalid"),
    SYMBOL_API_TRADING_NOT_AVAILABLE(-1157, "The trading pair is not available for api trading"),
    CREATE_LIMIT_MAKER_ORDER_FAILED(-1158, "create limit maker order failed"),


    ERROR_MODIFY_MARGIN(-1160, "Modify futures margin error"),
    REDUCE_MARGIN_FORBIDDEN(-1161, "Reduce margin forbidden"),

    FINANCE_ACCOUNT_EXIST(-1170, "finance account exist."),
    ACCOUNT_NOT_EXIST(-1171, "account not exist."),
    BALANCE_TRANSFER_FAILED(-1172, "Balance transfer failed."),
    WITHDRAW_ADDRESS_ILLEGAL(-1173, "Withdraw address illegal."),
    REPEATED_SUBMIT_REQUEST(-1174, "Repeated submit request."),
    UNSUPPORTED_CONTRACT_ADDRESS(-1175, "Not support contract address."),
    WITHDRAW_FAILED(-1176, "Withdraw failed, maybe occurred a error."),
    WITHDRAW_AMOUNT_CANNOT_BE_NULL(-1177, "Withdrawal amount cannot be null"),
    WITHDRAW_AMOUNT_MAX_LIMIT(-1178, "Withdraw amount exceeds the daily limit"),
    WITHDRAW_AMOUNT_MIN_LIMIT(-1179, "Withdraw amount less than the min withdraw amount limit"),
    WITHDRAW_AMOUNT_ILLEGAL(-1180, "Withdrawal amount illegal"),
    WITHDRAW_NOT_ALLOW(-1181, "Currently not allowed to withdraw."),
    DEPOSIT_NOT_ALLOW(-1182, "Currently not allowed to deposit."),
    WITHDRAW_ADDRESS_NOT_IN_WHITELIST(-1187, "Withdrawal address not in whitelist"),

    THIRD_USER_EXIST(-1181, "Third user exist"),
    THIRD_TOKEN_EXIST(-1182, "Third token exist"),
    BIND_THIRD_USER_ERROR(-1183, "Bind third user error"),
    GET_THIRD_TOKEN_ERROR(-1184, "GET third token error"),
    NO_PERMISSION_TO_CREATE_VIRTUAL_ACCOUNT(-1185, "No permission to create a virtual account"),
    THIRD_USER_NOT_EXIST(-1186, "Third user not exist"),
    ERROR_BUSINESS_SUBJECT(-1187, "error business subject"),

    NEW_ORDER_REJECTED(-2010, "New order rejected."),
    CANCEL_REJECTED(-2011, "CANCEL_REJECTED"),
    NO_SUCH_ORDER(-2013, "Order does not exist."),
    BAD_API_KEY_FMT(-2014, "API-key format invalid."),
    REJECTED_MBX_KEY(-2015, "Invalid API-key, IP, or permissions for action."),
    NO_TRADING_WINDOW(-2016, "No trading window could be found for the symbol. Try ticker/24hrs instead."),

    OPTION_NOT_EXIST(-3000, "Option not exist."),
    OPTION_HAS_EXPIRED(-3001, "The option has expired."),
    OPTION_ORDER_POSITION_LIMIT(-3002, " Order failed: position exceeded limit"),

    CREATE_API_KEY_EXCEED_LIMIT(-3050, "The ApiKey corresponding to the account already exists"),

    OPEN_MARGIN_ACCOUNT_ERROR(-3101,"open margin account error"),
    GET_MARGIN_SAFETY_ERROR(-3102,"get margin safety error"),
    RISK_IS_NOT_EXIT(-3103,"risk config is not exit"),
    GET_LOANABLE_ERROR(-3104,"get loanable error"),
    MARGIN_TOKEN_NOT_BORROW(-3105, "token can not borrow"),
    GET_LOAN_POSITION_ERROR(-3106, "get loan position error"),
    MARGIN_TOKEN_NOT_WITHDRAW(-3107, "token can not withdraw"),
    GET_AVAIL_WITHDRAW_ERROR(-3108, "get token avail withdraw error"),
    MARGIN_WITHDRAW_ERROR(-3109, "margin withdraw failed"),
    MARGIN_AVAIL_WITHDRAW_NOT_ENOUGH_FAILED(-3110, "margin avail withdraw not enough failed"),
    MARGIN_LOAN_AMOUNT_TOO_BIG_OR_SMALL(-3111, "loan amount is too big or small"),
    MARGIN_LOAN_AMOUNT_PRECISION_TOO_LONG(-3112, "loan amount precision is too long"),
    LOAN_ERROR(-3113, "loan fail"),
    MARGIN_LOAN_ORDER_NOT_EXIST(-3114, "loan order not exist"),
    MARGIN_LOAN_REPAY_AMOUNT_IS_SMALL(-3115, "repay amount is small"),
    REPAY_ERROR(-3116, "repay fail"),
    GET_MARGIN_ALL_POSITION_ERROR(-3117, "get margin all position fail"),
    GET_LOAN_ORDER_ERROR(-3118, "get loan order fail"),
    LOAN_ORDER_NOT_EXIT(-3119, "loan order is not exit"),
    GET_REPAY_ORDER_ERROR(-3120, "get repay order fail"),
    OTC_THIRD_PARTY_ORDER_NOT_EXIT(-3121, "third party order is not exit"),
    OTC_THIRD_PARTY_ADDRESS_ILLEGAL(-3122, "transfer wallet address illegal"),
    OTC_THIRD_PARTY_TRANSFER_AMOUNT_ILLEGAL(-3123, "transfer token amount illegal");





    private final int code;
    private final String msg;

    ApiErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public static void main(String[] args) {
        for (ApiErrorCode apiErrorCode : ApiErrorCode.values()) {
            if (apiErrorCode.code < -1139) {
                log.info("apiError:{}", apiErrorCode.code);
            }
        }
    }
}
