package com.example.demo.software.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SoftwareAssetResponse {

    private final Long id;
    private final String name;
    private final String version;
    private final String vendor;
    private final String categoryName;
    private final String status;
    private final String licenseType;
    private final Integer seats;
    private final BigDecimal annualCost;
    private final LocalDate maintenanceExpiryDate;
    private final Long daysUntilExpiry;
    private final boolean expired;
    private final boolean expiringSoon;
    private final boolean hasPrediction;

    public SoftwareAssetResponse(Long id,
                                 String name,
                                 String version,
                                 String vendor,
                                 String categoryName,
                                 String status,
                                 String licenseType,
                                 Integer seats,
                                 BigDecimal annualCost,
                                 LocalDate maintenanceExpiryDate,
                                 Long daysUntilExpiry,
                                 boolean expired,
                                 boolean expiringSoon,
                                 boolean hasPrediction) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.vendor = vendor;
        this.categoryName = categoryName;
        this.status = status;
        this.licenseType = licenseType;
        this.seats = seats;
        this.annualCost = annualCost;
        this.maintenanceExpiryDate = maintenanceExpiryDate;
        this.daysUntilExpiry = daysUntilExpiry;
        this.expired = expired;
        this.expiringSoon = expiringSoon;
        this.hasPrediction = hasPrediction;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getVendor() {
        return vendor;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getStatus() {
        return status;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public Integer getSeats() {
        return seats;
    }

    public BigDecimal getAnnualCost() {
        return annualCost;
    }

    public LocalDate getMaintenanceExpiryDate() {
        return maintenanceExpiryDate;
    }

    public Long getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isExpiringSoon() {
        return expiringSoon;
    }

    public boolean hasPrediction() {
        return hasPrediction;
    }
}
