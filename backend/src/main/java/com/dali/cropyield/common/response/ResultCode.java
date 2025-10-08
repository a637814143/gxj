package com.dali.cropyield.common.response;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS("OK"),
    BAD_REQUEST("请求参数错误"),
    NOT_FOUND("资源未找到"),
    SERVER_ERROR("服务异常");

    private final String defaultMessage;

    ResultCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }
}
