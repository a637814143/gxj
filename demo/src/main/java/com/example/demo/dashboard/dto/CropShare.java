package com.example.demo.dashboard.dto;

public record CropShare(
        String cropName,
        double production,
        double sownArea,
        double share
) {
}
