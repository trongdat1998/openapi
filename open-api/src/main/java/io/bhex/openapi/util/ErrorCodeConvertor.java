package io.bhex.openapi.util;

import io.bhex.broker.common.exception.BrokerErrorCode;
import io.bhex.openapi.constant.ApiErrorCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrorCodeConvertor {

    public static ApiErrorCode convert(BrokerErrorCode brokerErrorCode, ApiErrorCode defaultErrorCode) {
        if (brokerErrorCode == null) {
            log.warn("can not convert BrokerErrorCode for NULL. use default: {}", defaultErrorCode);
            return defaultErrorCode;
        }

        switch (brokerErrorCode) {
            case ORDER_REQUEST_SYMBOL_INVALID:
                return ApiErrorCode.BAD_SYMBOL;
            case ORDER_PRICE_TOO_HIGH:
                return ApiErrorCode.ORDER_PRICE_TOO_HIGH;
            case ORDER_PRICE_TOO_SMALL:
                return ApiErrorCode.ORDER_PRICE_TOO_SMALL;
            case ORDER_PRICE_PRECISION_TOO_LONG:
                return ApiErrorCode.ORDER_PRICE_PRECISION_TOO_LONG;
            case ORDER_QUANTITY_TOO_BIG:
                return ApiErrorCode.ORDER_QUANTITY_TOO_BIG;
            case ORDER_QUANTITY_TOO_SMALL:
                return ApiErrorCode.ORDER_QUANTITY_TOO_SMALL;
            case ORDER_QUANTITY_PRECISION_TOO_LONG:
                return ApiErrorCode.ORDER_QUANTITY_PRECISION_TOO_LONG;
            case ORDER_PRICE_WAVE_EXCEED:
                return ApiErrorCode.ORDER_PRICE_WAVE_EXCEED;
            case ORDER_AMOUNT_TOO_SMALL:
                return ApiErrorCode.ORDER_AMOUNT_TOO_SMALL;
            case ORDER_AMOUNT_PRECISION_TOO_LONG:
                return ApiErrorCode.ORDER_AMOUNT_PRECISION_TOO_LONG;
            case ORDER_FUTURES_QUANTITY_INVALID:
                return ApiErrorCode.ORDER_QUANTITY_INVALID;
            case INSUFFICIENT_BALANCE:
                return ApiErrorCode.INSUFFICIENT_BALANCE;
            case TRANSFER_INSUFFICIENT_BALANCE:
                return ApiErrorCode.INSUFFICIENT_BALANCE;
            case DUPLICATED_ORDER:
                return ApiErrorCode.DUPLICATED_ORDER;
            case ORDER_FAILED:
                return ApiErrorCode.CREATE_ORDER_FAILED;
            case CREATE_ORDER_TIMEOUT:
                return ApiErrorCode.CREATE_ORDER_TIMEOUT;
            case OPTION_NOT_EXIST:
                return ApiErrorCode.OPTION_NOT_EXIST;
            case OPTION_HAS_EXPIRED:
                return ApiErrorCode.OPTION_HAS_EXPIRED;
            case ERR_CANCEL_ORDER_POSITION_LIMIT:
                return ApiErrorCode.OPTION_ORDER_POSITION_LIMIT;
            case ORDER_HAS_BEEN_FILLED:
            case CANCEL_ORDER_FINISHED:
                return ApiErrorCode.ORDER_HAS_FILLED;
            case ORDER_NOT_FOUND:
                return ApiErrorCode.NO_SUCH_ORDER;
            case CANCEL_ORDER_CANCELLED:
                return ApiErrorCode.ORDER_CANCELLED;
            case CANCEL_ORDER_REJECTED:
            case CANCEL_ORDER_ARCHIVED:
                return ApiErrorCode.ORDER_NOT_FOUND_ON_ORDER_BOOK;
            case CANCEL_ORDER_LOCKED:
                return ApiErrorCode.ORDER_LOCKED;
            case CANCEL_ORDER_UNSUPPORTED_ORDER_TYPE:
                return ApiErrorCode.UNSUPPORTED_ORDER_TYPE_UNSUPPORTED_CANCEL;
            case CANCEL_ORDER_TIMEOUT:
                return ApiErrorCode.CANCEL_ORDER_TIMEOUT;
            case ERR_REDUCE_MARGIN_FORBIDDEN:
                return ApiErrorCode.REDUCE_MARGIN_FORBIDDEN;
            case FINANCE_ACCOUNT_EXIST:
                return ApiErrorCode.FINANCE_ACCOUNT_EXIST;
            case SUB_ACCOUNT_TRANSFER_FAILED:
            case ACCOUNT_TRANSFER_MUST_BE_MAIN_ACCOUNT:
            case BALANCE_TRANSFER_FAILED:
                return ApiErrorCode.BALANCE_TRANSFER_FAILED;
            case ACCOUNT_NOT_EXIST:
                return ApiErrorCode.ACCOUNT_NOT_EXIST;
            case FEATURE_SUSPENDED:
                return ApiErrorCode.FEATURE_SUSPENDED;
            case ORDER_FROZEN_BY_ADMIN:
                return ApiErrorCode.NEW_ORDER_REJECTED;
            case OPERATION_HAS_NO_PERMISSION:
                return ApiErrorCode.NO_PERMISSION;
            case CREATE_API_KEY_EXCEED_LIMIT:
                return ApiErrorCode.CREATE_API_KEY_EXCEED_LIMIT;
            case ORDER_LIMIT_MAKER_FAILED:
                return ApiErrorCode.CREATE_LIMIT_MAKER_ORDER_FAILED;
            case SYMBOL_OPENAPI_TRADE_FORBIDDEN:
                return ApiErrorCode.SYMBOL_API_TRADING_NOT_AVAILABLE;
            case DEPOSIT_NOT_ALLOW:
                return ApiErrorCode.DEPOSIT_NOT_ALLOW;
            case WITHDRAW_NOT_ALLOW:
                return ApiErrorCode.WITHDRAW_NOT_ALLOW;
            default:
                return defaultErrorCode;
        }
    }
}
