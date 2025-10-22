package com.gxj.cropyield.common.exception;

import com.gxj.cropyield.common.response.ResultCode;
/**
 * 统一异常模块的异常类，用于标识统一异常流程中的错误状态。
 */

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
