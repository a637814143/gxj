package com.gxj.demo.software.dto;

import java.math.BigDecimal;

public class CategorySummaryResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final long softwareCount;
    private final long activeSoftwareCount;
    private final long predictedSoftwareCount;
    private final BigDecimal totalAnnualCost;

    public CategorySummaryResponse(Long id,
                                   String name,
                                   String description,
                                   long softwareCount,
                                   long activeSoftwareCount,
                                   long predictedSoftwareCount,
                                   BigDecimal totalAnnualCost) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.softwareCount = softwareCount;
        this.activeSoftwareCount = activeSoftwareCount;
        this.predictedSoftwareCount = predictedSoftwareCount;
        this.totalAnnualCost = totalAnnualCost;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getSoftwareCount() {
        return softwareCount;
    }

    public long getActiveSoftwareCount() {
        return activeSoftwareCount;
    }

    public long getPredictedSoftwareCount() {
        return predictedSoftwareCount;
    }

    public BigDecimal getTotalAnnualCost() {
        return totalAnnualCost;
    }
}
