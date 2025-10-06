package nongye.example.demo.controller;

import nongye.example.demo.entity.CropData;
import nongye.example.demo.service.CropDataService;
import nongye.example.demo.service.DataImportService;
import nongye.example.demo.service.FileParsingService;
import nongye.example.demo.dto.DataImportRequest;
import nongye.example.demo.dto.DataImportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crop-data")
@CrossOrigin(origins = "*")
@Slf4j
public class CropDataController {
    
    @Autowired
    private CropDataService cropDataService;
    
    @Autowired
    private DataImportService dataImportService;
    
    @Autowired
    private FileParsingService fileParsingService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<CropData>> getAllCropData() {
        List<CropData> cropData = cropDataService.getAllCropData();
        return ResponseEntity.ok(cropData);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<CropData> getCropDataById(@PathVariable Long id) {
        CropData cropData = cropDataService.getCropDataById(id);
        return ResponseEntity.ok(cropData);
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CropData> createCropData(@Valid @RequestBody CropData cropData) {
        CropData createdCropData = cropDataService.saveCropData(cropData);
        return ResponseEntity.ok(createdCropData);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CropData> updateCropData(@PathVariable Long id, @Valid @RequestBody CropData cropData) {
        cropData.setId(id);
        CropData updatedCropData = cropDataService.saveCropData(cropData);
        return ResponseEntity.ok(updatedCropData);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCropData(@PathVariable Long id) {
        cropDataService.deleteCropData(id);
        return ResponseEntity.ok("作物数据删除成功");
    }
    
    @GetMapping("/region/{regionCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<CropData>> getCropDataByRegion(@PathVariable String regionCode) {
        List<CropData> cropData = cropDataService.getCropDataByRegion(regionCode);
        return ResponseEntity.ok(cropData);
    }
    
    @GetMapping("/crop-type/{cropType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<CropData>> getCropDataByCropType(@PathVariable String cropType) {
        List<CropData> cropData = cropDataService.getCropDataByCropType(cropType);
        return ResponseEntity.ok(cropData);
    }
    
    @GetMapping("/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<CropData>> getCropDataByYear(@PathVariable Integer year) {
        List<CropData> cropData = cropDataService.getCropDataByYear(year);
        return ResponseEntity.ok(cropData);
    }
    
    @GetMapping("/region/{regionCode}/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<CropData>> getCropDataByRegionAndYear(@PathVariable String regionCode, 
                                                                   @PathVariable Integer year) {
        List<CropData> cropData = cropDataService.getCropDataByRegionAndYear(regionCode, year);
        return ResponseEntity.ok(cropData);
    }
    
    @GetMapping("/region/{regionCode}/crop-type/{cropType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<CropData>> getCropDataByRegionAndCropType(@PathVariable String regionCode, 
                                                                        @PathVariable String cropType) {
        List<CropData> cropData = cropDataService.getCropDataByRegionAndCropType(regionCode, cropType);
        return ResponseEntity.ok(cropData);
    }
    
    @GetMapping("/region/{regionCode}/crop-type/{cropType}/year-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<CropData>> getCropDataByRegionAndCropAndYearRange(
            @PathVariable String regionCode, 
            @PathVariable String cropType,
            @RequestParam Integer startYear,
            @RequestParam Integer endYear) {
        List<CropData> cropData = cropDataService.getCropDataByRegionAndCropAndYearRange(regionCode, cropType, startYear, endYear);
        return ResponseEntity.ok(cropData);
    }
    
    @GetMapping("/regions")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<String>> getDistinctRegionCodes() {
        List<String> regionCodes = cropDataService.getDistinctRegionCodes();
        return ResponseEntity.ok(regionCodes);
    }
    
    @GetMapping("/crop-types")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<String>> getDistinctCropTypes() {
        List<String> cropTypes = cropDataService.getDistinctCropTypes();
        return ResponseEntity.ok(cropTypes);
    }
    
    @GetMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<List<Integer>> getDistinctYears() {
        List<Integer> years = cropDataService.getDistinctYears();
        return ResponseEntity.ok(years);
    }
    
    @GetMapping("/statistics/yield/region/{regionCode}/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<Map<String, BigDecimal>> getYieldStatisticsByRegion(@PathVariable String regionCode, 
                                                                             @PathVariable Integer year) {
        Map<String, BigDecimal> statistics = cropDataService.getYieldStatisticsByRegion(regionCode, year);
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/statistics/trend/region/{regionCode}/crop-type/{cropType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<Map<Integer, BigDecimal>> getYieldTrendByRegionAndCrop(
            @PathVariable String regionCode, 
            @PathVariable String cropType,
            @RequestParam Integer startYear,
            @RequestParam Integer endYear) {
        Map<Integer, BigDecimal> trend = cropDataService.getYieldTrendByRegionAndCrop(regionCode, cropType, startYear, endYear);
        return ResponseEntity.ok(trend);
    }
    
    @GetMapping("/statistics/area/region/{regionCode}/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<Map<String, BigDecimal>> getAreaStatisticsByRegion(@PathVariable String regionCode, 
                                                                            @PathVariable Integer year) {
        Map<String, BigDecimal> statistics = cropDataService.getAreaStatisticsByRegion(regionCode, year);
        return ResponseEntity.ok(statistics);
    }
    
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadCropData(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "dataSource", defaultValue = "文件导入") String dataSource,
                                          @RequestParam(value = "cleanData", defaultValue = "true") Boolean cleanData,
                                          @RequestParam(value = "validateData", defaultValue = "true") Boolean validateData,
                                          @RequestParam(value = "description", required = false) String description,
                                          @RequestParam(value = "importType", defaultValue = "FULL") String importType) {
        
        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "请选择要上传的文件"));
            }
            
            if (!dataImportService.isValidFileFormat(file)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "不支持的文件格式，支持的格式：" + String.join(", ", dataImportService.getSupportedFormats())
                ));
            }
            
            if (file.getSize() > dataImportService.getMaxFileSize()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "文件大小超过限制（最大50MB）"
                ));
            }
            
            // 构建导入请求
            DataImportRequest request = new DataImportRequest();
            request.setDataSource(dataSource);
            request.setCleanData(cleanData);
            request.setValidateData(validateData);
            request.setDescription(description);
            request.setImportType(importType);
            
            // 执行导入
            DataImportResponse response;
            if (file.getSize() > 10 * 1024 * 1024) { // 大于10MB使用批量导入
                response = dataImportService.batchImportData(file, request);
            } else {
                response = dataImportService.importData(file, request);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "文件上传失败：" + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/template/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER')")
    public ResponseEntity<String> downloadCsvTemplate() {
        try {
            String template = fileParsingService.generateCsvTemplate();
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"crop_data_template.csv\"")
                .body(template);
                
        } catch (Exception e) {
            log.error("下载CSV模板失败", e);
            return ResponseEntity.status(500).body("下载模板失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/import/formats")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER')")
    public ResponseEntity<Map<String, Object>> getSupportedFormats() {
        Map<String, Object> info = new HashMap<>();
        info.put("supportedFormats", dataImportService.getSupportedFormats());
        info.put("maxFileSize", dataImportService.getMaxFileSize());
        info.put("maxFileSizeMB", dataImportService.getMaxFileSize() / (1024 * 1024));
        
        return ResponseEntity.ok(info);
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<Map<String, Object>> getDataStatistics() {
        try {
            List<CropData> allData = cropDataService.getAllCropData();
            List<String> regions = cropDataService.getDistinctRegionCodes();
            List<String> cropTypes = cropDataService.getDistinctCropTypes();
            List<Integer> years = cropDataService.getDistinctYears();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalRecords", allData.size());
            statistics.put("regions", regions.size());
            statistics.put("cropTypes", cropTypes.size());
            statistics.put("years", years.size());
            statistics.put("yearRange", years.isEmpty() ? new int[]{0, 0} : 
                          new int[]{years.stream().mapToInt(i -> i).min().orElse(0), 
                                   years.stream().mapToInt(i -> i).max().orElse(0)});
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("totalRecords", 0);
            error.put("regions", 0);
            error.put("cropTypes", 0);
            error.put("years", 0);
            error.put("error", e.getMessage());
            return ResponseEntity.ok(error);
        }
    }
    
    @GetMapping("/regions/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESEARCHER', 'FARMER')")
    public ResponseEntity<Map<String, String>> getRegionNamesAndCodes() {
        try {
            List<String> regionCodes = cropDataService.getDistinctRegionCodes();
            List<CropData> sampleData = cropDataService.getAllCropData();
            
            Map<String, String> regionMap = new HashMap<>();
            for (CropData data : sampleData) {
                if (!regionMap.containsKey(data.getRegionCode())) {
                    regionMap.put(data.getRegionCode(), data.getRegionName());
                }
            }
            
            return ResponseEntity.ok(regionMap);
        } catch (Exception e) {
            log.error("获取地区信息失败", e);
            return ResponseEntity.ok(new HashMap<>());
        }
    }
}
