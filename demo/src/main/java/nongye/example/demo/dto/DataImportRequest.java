package nongye.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataImportRequest {
    
    @NotBlank(message = "数据源不能为空")
    private String dataSource; // 数据来源
    
    @NotNull(message = "是否清洗数据标志不能为空")
    private Boolean cleanData = true; // 是否进行数据清洗
    
    @NotNull(message = "是否验证数据标志不能为空")
    private Boolean validateData = true; // 是否进行数据验证
    
    private String description; // 导入描述
    
    private String importType; // 导入类型：FULL（全量）、INCREMENTAL（增量）
}
