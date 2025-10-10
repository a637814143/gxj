package com.example.demo.prediction;

import com.example.demo.software.SoftwareAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "usage_predictions")
public class UsagePrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    private SoftwareAsset software;

    @Column(name = "prediction_date", nullable = false)
    private LocalDate predictionDate;

    @Column(name = "predicted_annual_cost", precision = 12, scale = 2)
    private BigDecimal predictedAnnualCost;

    @Column(name = "predicted_active_users")
    private Integer predictedActiveUsers;

    @Column(precision = 4, scale = 2)
    private BigDecimal confidence;

    @Column(length = 500)
    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SoftwareAsset getSoftware() {
        return software;
    }

    public void setSoftware(SoftwareAsset software) {
        this.software = software;
    }

    public LocalDate getPredictionDate() {
        return predictionDate;
    }

    public void setPredictionDate(LocalDate predictionDate) {
        this.predictionDate = predictionDate;
    }

    public BigDecimal getPredictedAnnualCost() {
        return predictedAnnualCost;
    }

    public void setPredictedAnnualCost(BigDecimal predictedAnnualCost) {
        this.predictedAnnualCost = predictedAnnualCost;
    }

    public Integer getPredictedActiveUsers() {
        return predictedActiveUsers;
    }

    public void setPredictedActiveUsers(Integer predictedActiveUsers) {
        this.predictedActiveUsers = predictedActiveUsers;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
