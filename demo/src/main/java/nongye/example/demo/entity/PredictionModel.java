package nongye.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "model_name", nullable = false)
    private String modelName;
    
    @Column(name = "model_type", nullable = false)
    private String modelType; // ARIMA, Prophet, XGBoost, LSTM
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "model_file_path")
    private String modelFilePath;
    
    @Column(name = "parameters")
    private String parameters; // JSON格式存储模型参数
    
    @Column(name = "accuracy")
    private Double accuracy; // 模型准确率
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ModelStatus status = ModelStatus.ACTIVE;
    
    @Column(name = "created_by")
    private Long createdBy;
    
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
    
    public enum ModelStatus {
        ACTIVE, INACTIVE, TRAINING, ERROR
    }
}
