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
    

    String operation();
    

    String module();

    String description() default "";
    

    boolean recordParams() default true;
    

    boolean recordResult() default false;
}
