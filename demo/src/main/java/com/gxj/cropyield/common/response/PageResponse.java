package com.gxj.cropyield.common.response;

import java.util.List;
/**
 * 通用响应模块的数据传输对象（记录类型），在通用响应场景下承载参数与返回值。
 */

public record PageResponse<T>(List<T> records, long total, int page, int size) {
}
