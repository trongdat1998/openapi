package io.bhex.openapi.constant;

public class OpenApiException extends RuntimeException {

    private int code;

    private String msg;

    private String[] messageArgs;

    public OpenApiException(ApiErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public OpenApiException(ApiErrorCode errorCode, String... args) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
        this.messageArgs = args;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return msg;
    }

    public String[] getMessageArgs() {
        return messageArgs;
    }
}
