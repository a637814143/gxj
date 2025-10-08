package com.dali.cropyield.modules.forecast.entity;

import com.dali.cropyield.common.model.BaseEntity;
import com.dali.cropyield.modules.base.entity.Crop;
import com.dali.cropyield.modules.base.entity.Region;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fc_task")
public class ForecastTask extends BaseEntity {

    public enum Status {
        CREATED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private ForecastModel model;

    @Column(nullable = false)
    private Integer startYear;

    @Column(nullable = false)
    private Integer endYear;

    @Column(nullable = false)
    private Integer horizonYears;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.CREATED;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(length = 256)
    private String errorMessage;

    @OneToMany(mappedBy = "task")
    @JsonIgnoreProperties({"task"})
    private Set<ForecastResult> results = new HashSet<>();
}
