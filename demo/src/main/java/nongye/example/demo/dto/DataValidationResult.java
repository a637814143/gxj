package nongye.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataValidationResult {
    
    private Boolean isValid; // 数据是否有效
    
    private List<String> errors; // 错误信息
    
    private List<String> warnings; // 警告信息
    
    private Integer rowNumber; // 行号
    
    private String fieldName; // 字段名
    
    private Object originalValue; // 原始值
    
    private Object cleanedValue; // 清洗后的值
    
    private String validationType; // 验证类型
}
