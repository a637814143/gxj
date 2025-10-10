package com.gxj.cropyield.dashboard.dto;

public record CropShare(
        String cropName,
        double production,
        double sownArea,
        double share
) {
}
