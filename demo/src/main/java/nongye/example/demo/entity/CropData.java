package nongye.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crop_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "region_code", nullable = false)
    private String regionCode;
    
    @Column(name = "region_name", nullable = false)
    private String regionName;
    
    @Column(name = "crop_type", nullable = false)
    private String cropType;
    
    @Column(name = "crop_name", nullable = false)
    private String cropName;
    
    @Column(name = "planting_area")
    private BigDecimal plantingArea; // 种植面积（亩）
    
    @Column(name = "yield")
    private BigDecimal yield; // 产量（吨）
    
    @Column(name = "unit_yield")
    private BigDecimal unitYield; // 单产（吨/亩）
    
    @Column(name = "price")
    private BigDecimal price; // 价格（元/吨）
    
    @Column(name = "total_value")
    private BigDecimal totalValue; // 总产值（元）
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "season")
    private String season; // 季节：春、夏、秋、冬
    
    @Column(name = "weather_condition")
    private String weatherCondition; // 天气条件
    
    @Column(name = "pest_disease_level")
    private String pestDiseaseLevel; // 病虫害程度
    
    @Column(name = "fertilizer_usage")
    private BigDecimal fertilizerUsage; // 化肥使用量（吨）
    
    @Column(name = "pesticide_usage")
    private BigDecimal pesticideUsage; // 农药使用量（吨）
    
    @Column(name = "irrigation_area")
    private BigDecimal irrigationArea; // 灌溉面积（亩）
    
    @Column(name = "data_source")
    private String dataSource; // 数据来源
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
