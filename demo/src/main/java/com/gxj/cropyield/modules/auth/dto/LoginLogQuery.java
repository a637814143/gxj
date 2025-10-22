package com.gxj.cropyield.modules.auth.dto;

import java.time.LocalDate;
/**
 * 认证与账号模块的数据传输对象（记录类型），在认证与账号场景下承载参数与返回值。
 */

public record LoginLogQuery(
    String username,
    Boolean success,
    LocalDate startDate,
    LocalDate endDate
) {
}
