package com.gxj.cropyield.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志切面
 * 
 * 自动拦截带有@AuditLog注解的方法
 * 记录操作信息到数据库和日志文件
 */
@Aspect
@Component
public class AuditLogAspect {
    
    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    public AuditLogAspect(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }
    
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 创建审计日志实体
        AuditLogEntity entity = new AuditLogEntity();
        entity.setOperation(auditLog.operation());
        entity.setModule(auditLog.module());
        entity.setDescription(auditLog.description());
        entity.setCreatedAt(LocalDateTime.now());
        
        // 获取当前用户
        String username = getCurrentUsername();
        entity.setUsername(username);
        
        // 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            entity.setIpAddress(getClientIp(request));
            entity.setUserAgent(request.getHeader("User-Agent"));
            entity.setRequestUri(request.getRequestURI());
            entity.setRequestMethod(request.getMethod());
        }
        
        // 记录请求参数
        if (auditLog.recordParams()) {
            try {
                String params = getMethodParams(joinPoint);
                entity.setRequestParams(params);
            } catch (Exception e) {
                log.warn("记录请求参数失败: {}", e.getMessage());
            }
        }
        
        // 执行方法
        Object result = null;
        try {
            result = joinPoint.proceed();
            entity.setResult("SUCCESS");
            
            // 记录返回结果
            if (auditLog.recordResult() && result != null) {
                try {
                    String resultJson = objectMapper.writeValueAsString(result);
                    if (resultJson.length() > 1000) {
                        resultJson = resultJson.substring(0, 1000) + "...";
                    }
                    entity.setErrorMessage(resultJson); // 复用字段存储结果
                } catch (Exception e) {
                    log.warn("记录返回结果失败: {}", e.getMessage());
                }
            }
            
        } catch (Throwable e) {
            entity.setResult("FAILURE");
            entity.setErrorMessage(e.getMessage());
            throw e;
            
        } finally {
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            entity.setExecutionTime(executionTime);
            
            // 保存审计日志到数据库
            try {
                auditLogRepository.save(entity);
            } catch (Exception e) {
                log.error("保存审计日志失败: {}", e.getMessage(), e);
            }
            
            // 写入审计日志文件
            writeAuditLog(entity);
        }
        
        return result;
    }
    
    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String) {
                    return (String) principal;
                }
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("获取当前用户失败: {}", e.getMessage());
        }
        return "anonymous";
    }
    
    /**
     * 获取HTTP请求对象
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("获取HTTP请求失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 获取方法参数
     */
    private String getMethodParams(ProceedingJoinPoint joinPoint) throws Exception {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            // 过滤敏感参数
            String paramName = paramNames[i];
            if (isSensitiveParam(paramName)) {
                params.put(paramName, "******");
            } else {
                Object value = paramValues[i];
                // 避免记录过大的对象
                if (value != null && value.toString().length() > 500) {
                    params.put(paramName, value.getClass().getSimpleName() + "(省略)");
                } else {
                    params.put(paramName, value);
                }
            }
        }
        
        String json = objectMapper.writeValueAsString(params);
        // 限制参数长度
        if (json.length() > 2000) {
            json = json.substring(0, 2000) + "...";
        }
        return json;
    }
    
    /**
     * 判断是否为敏感参数
     */
    private boolean isSensitiveParam(String paramName) {
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password") 
            || lowerName.contains("pwd")
            || lowerName.contains("secret")
            || lowerName.contains("token")
            || lowerName.contains("key");
    }
    
    /**
     * 写入审计日志文件
     */
    private void writeAuditLog(AuditLogEntity entity) {
        try {
            String logMessage = String.format(
                "[%s] 用户: %s, 操作: %s, 模块: %s, 描述: %s, 结果: %s, 耗时: %dms, IP: %s",
                entity.getCreatedAt(),
                entity.getUsername(),
                entity.getOperation(),
                entity.getModule(),
                entity.getDescription(),
                entity.getResult(),
                entity.getExecutionTime(),
                entity.getIpAddress()
            );
            
            if ("SUCCESS".equals(entity.getResult())) {
                auditLogger.info(logMessage);
            } else {
                auditLogger.error(logMessage + ", 错误: " + entity.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("写入审计日志文件失败: {}", e.getMessage());
        }
    }
}
