package com.example.demo.crop;

public record CropResponse(
        Long id,
        String name,
        String category,
        String description
) {
}
