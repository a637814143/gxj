package com.gxj.cropyield.common.exception;

import com.gxj.cropyield.common.response.ResultCode;

public class BusinessException extends RuntimeException {

    private final ResultCode code;

    public BusinessException(ResultCode code, String message) {
        super(message);
        this.code = code;
    }

    public ResultCode getCode() {
        return code;
    }
}
