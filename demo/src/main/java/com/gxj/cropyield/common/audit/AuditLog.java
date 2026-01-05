package com.gxj.cropyield.common.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解
 * 
 * 用于标记需要记录审计日志的方法
 * 通过AOP自动记录操作信息
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    
    /**
     * 操作类型
     * 例如：CREATE_USER, UPDATE_USER, DELETE_USER, LOGIN, IMPORT_DATA等
     */
    String operation();
    
    /**
     * 模块名称
     * 例如：用户管理、数据管理、预测管理等
     */
    String module();
    
    /**
     * 操作描述
     * 详细描述操作内容
     */
    String description() default "";
    
    /**
     * 是否记录请求参数
     * 默认true，敏感操作可设置为false
     */
    boolean recordParams() default true;
    
    /**
     * 是否记录返回结果
     * 默认false，避免记录大量数据
     */
    boolean recordResult() default false;
}
