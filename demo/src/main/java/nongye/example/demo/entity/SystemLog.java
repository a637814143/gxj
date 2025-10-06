package nongye.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "operation", nullable = false)
    private String operation; // 操作类型
    
    @Column(name = "module", nullable = false)
    private String module; // 模块名称
    
    @Column(name = "description")
    private String description; // 操作描述
    
    @Column(name = "ip_address")
    private String ipAddress; // IP地址
    
    @Column(name = "user_agent")
    private String userAgent; // 用户代理
    
    @Column(name = "request_url")
    private String requestUrl; // 请求URL
    
    @Column(name = "request_method")
    private String requestMethod; // 请求方法
    
    @Column(name = "request_params")
    private String requestParams; // 请求参数
    
    @Column(name = "response_status")
    private Integer responseStatus; // 响应状态码
    
    @Column(name = "execution_time")
    private Long executionTime; // 执行时间（毫秒）
    
    @Column(name = "log_level")
    @Enumerated(EnumType.STRING)
    private LogLevel logLevel = LogLevel.INFO;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
