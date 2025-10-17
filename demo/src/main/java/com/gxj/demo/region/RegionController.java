package com.gxj.demo.region;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionRepository regionRepository;

    public RegionController(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @GetMapping
    public List<RegionResponse> listAll() {
        return regionRepository.findAll().stream()
                .map(region -> new RegionResponse(
                        region.getId(),
                        region.getName(),
                        region.getLevel(),
                        region.getParentName(),
                        region.getDescription()
                ))
                .toList();
    }
}
