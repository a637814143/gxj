package com.gxj.cropyield.datamanagement.model;
/**
 * 数据导入模块的模型定义（枚举），描述数据导入领域的公共数据结构。
 */

public enum DataImportJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED
}
