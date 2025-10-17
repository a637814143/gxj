package com.gxj.demo.software;

import com.gxj.demo.software.dto.CategorySummaryResponse;
import com.gxj.demo.software.dto.SoftwareAssetResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/software")
public class SoftwareAssetController {

    private final SoftwareAssetService softwareAssetService;

    public SoftwareAssetController(SoftwareAssetService softwareAssetService) {
        this.softwareAssetService = softwareAssetService;
    }

    @GetMapping
    public List<SoftwareAssetResponse> listSoftwareAssets(@RequestParam(required = false) String search,
                                                          @RequestParam(required = false) Long categoryId,
                                                          @RequestParam(required = false) SoftwareStatus status,
                                                          @RequestParam(required = false) Boolean expiringSoon) {
        return softwareAssetService.getSoftwareAssets(search, categoryId, status, expiringSoon);
    }

    @GetMapping("/categories")
    public List<CategorySummaryResponse> listCategorySummaries() {
        return softwareAssetService.getCategorySummaries();
    }
}
