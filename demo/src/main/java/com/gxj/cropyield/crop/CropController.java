package com.gxj.cropyield.crop;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("legacyCropController")
@RequestMapping("/api/crops")
public class CropController {

    private final CropRepository cropRepository;

    public CropController(CropRepository cropRepository) {
        this.cropRepository = cropRepository;
    }

    @GetMapping
    public List<CropResponse> listAll() {
        return cropRepository.findAll().stream()
                .map(crop -> new CropResponse(
                        crop.getId(),
                        crop.getName(),
                        crop.getCategory(),
                        crop.getDescription()
                ))
                .toList();
    }
}
