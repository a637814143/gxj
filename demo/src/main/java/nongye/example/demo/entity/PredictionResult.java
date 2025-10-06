package nongye.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "model_id", nullable = false)
    private Long modelId;
    
    @Column(name = "region_code", nullable = false)
    private String regionCode;
    
    @Column(name = "crop_type", nullable = false)
    private String cropType;
    
    @Column(name = "prediction_year", nullable = false)
    private Integer predictionYear;
    
    @Column(name = "prediction_season")
    private String predictionSeason;
    
    @Column(name = "predicted_yield")
    private BigDecimal predictedYield; // 预测产量
    
    @Column(name = "predicted_area")
    private BigDecimal predictedArea; // 预测种植面积
    
    @Column(name = "predicted_price")
    private BigDecimal predictedPrice; // 预测价格
    
    @Column(name = "confidence_interval_lower")
    private BigDecimal confidenceIntervalLower; // 置信区间下限
    
    @Column(name = "confidence_interval_upper")
    private BigDecimal confidenceIntervalUpper; // 置信区间上限
    
    @Column(name = "accuracy_score")
    private Double accuracyScore; // 预测准确度评分
    
    @Column(name = "scenario_name")
    private String scenarioName; // 情景名称
    
    @Column(name = "scenario_parameters")
    private String scenarioParameters; // 情景参数（JSON格式）
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", insertable = false, updatable = false)
    private PredictionModel predictionModel;
    
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
