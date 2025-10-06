package nongye.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataImportResponse {
    
    private Boolean success; // 导入是否成功
    
    private String message; // 响应消息
    
    private Integer totalRecords; // 总记录数
    
    private Integer successRecords; // 成功导入记录数
    
    private Integer failedRecords; // 失败记录数
    
    private Integer duplicateRecords; // 重复记录数
    
    private Integer cleanedRecords; // 清洗的记录数
    
    private List<String> errors; // 错误信息列表
    
    private List<String> warnings; // 警告信息列表
    
    private LocalDateTime importTime; // 导入时间
    
    private Long processingTimeMs; // 处理时间（毫秒）
    
    private String fileName; // 文件名
    
    private Long fileSize; // 文件大小（字节）
}
