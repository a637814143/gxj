package com.gxj.cropyield.region;

public record RegionResponse(
        Long id,
        String name,
        String level,
        String parentName,
        String description
) {
}
