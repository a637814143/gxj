package com.gxj.cropyield.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * 通用响应模块的数据传输对象（记录类型），在通用响应场景下承载参数与返回值。
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static ApiResponse<Void> failure(ResultCode code, String message) {
        return new ApiResponse<>(code.getCode(), message, null);
    }
}
