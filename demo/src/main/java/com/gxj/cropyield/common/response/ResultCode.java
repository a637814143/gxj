package com.gxj.cropyield.common.response;

public enum ResultCode {
    SUCCESS(0, "success"),
    BAD_REQUEST(400, "bad_request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not_found"),
    SERVER_ERROR(500, "server_error");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
