package com.gxj.demo.region;

public record RegionResponse(
        Long id,
        String name,
        String level,
        String parentName,
        String description
) {
}
