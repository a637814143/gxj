package nongye.example.demo.service;

import nongye.example.demo.entity.CropData;
import nongye.example.demo.dto.DataImportRequest;
import nongye.example.demo.dto.DataImportResponse;
import nongye.example.demo.dto.DataValidationResult;
import nongye.example.demo.repository.CropDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataImportService {
    
    @Autowired
    private FileParsingService fileParsingService;
    
    @Autowired
    private DataCleaningService dataCleaningService;
    
    @Autowired
    private CropDataRepository cropDataRepository;
    
    /**
     * 导入数据
     */
    @Transactional
    public DataImportResponse importData(MultipartFile file, DataImportRequest request) {
        long startTime = System.currentTimeMillis();
        
        DataImportResponse.DataImportResponseBuilder responseBuilder = DataImportResponse.builder()
            .importTime(LocalDateTime.now())
            .fileName(file.getOriginalFilename())
            .fileSize(file.getSize())
            .errors(new ArrayList<>())
            .warnings(new ArrayList<>());
        
        try {
            // 1. 解析文件
            log.info("开始解析文件: {}", file.getOriginalFilename());
            List<CropData> parsedData = fileParsingService.parseFile(file);
            responseBuilder.totalRecords(parsedData.size());
            
            if (parsedData.isEmpty()) {
                return responseBuilder
                    .success(false)
                    .message("文件中没有有效数据")
                    .successRecords(0)
                    .failedRecords(0)
                    .duplicateRecords(0)
                    .cleanedRecords(0)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            }
            
            // 2. 数据清洗和验证
            List<CropData> validData = new ArrayList<>();
            List<String> allErrors = new ArrayList<>();
            List<String> allWarnings = new ArrayList<>();
            int cleanedCount = 0;
            
            if (request.getCleanData() || request.getValidateData()) {
                log.info("开始数据清洗和验证");
                
                for (int i = 0; i < parsedData.size(); i++) {
                    CropData cropData = parsedData.get(i);
                    List<DataValidationResult> validationResults = 
                        dataCleaningService.cleanCropData(cropData, i + 2); // +2 因为从第2行开始（跳过标题行）
                    
                    boolean hasErrors = false;
                    for (DataValidationResult result : validationResults) {
                        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                            hasErrors = true;
                            allErrors.addAll(result.getErrors().stream()
                                .map(error -> "第" + result.getRowNumber() + "行 " + result.getFieldName() + ": " + error)
                                .collect(Collectors.toList()));
                        }
                        
                        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
                            allWarnings.addAll(result.getWarnings().stream()
                                .map(warning -> "第" + result.getRowNumber() + "行 " + result.getFieldName() + ": " + warning)
                                .collect(Collectors.toList()));
                            
                            if (result.getCleanedValue() != null) {
                                cleanedCount++;
                            }
                        }
                    }
                    
                    if (!hasErrors) {
                        validData.add(cropData);
                    }
                }
            } else {
                validData = parsedData;
            }
            
            // 3. 检查重复数据
            log.info("检查重复数据");
            List<CropData> uniqueData = new ArrayList<>();
            int duplicateCount = 0;
            
            for (CropData cropData : validData) {
                if (!isDuplicate(cropData)) {
                    uniqueData.add(cropData);
                } else {
                    duplicateCount++;
                    allWarnings.add("发现重复数据: " + cropData.getRegionName() + " - " + 
                                  cropData.getCropName() + " - " + cropData.getYear());
                }
            }
            
            // 4. 保存数据
            log.info("保存数据到数据库");
            List<CropData> savedData = cropDataRepository.saveAll(uniqueData);
            
            // 5. 构建响应
            long processingTime = System.currentTimeMillis() - startTime;
            
            return responseBuilder
                .success(true)
                .message("数据导入完成")
                .successRecords(savedData.size())
                .failedRecords(parsedData.size() - validData.size())
                .duplicateRecords(duplicateCount)
                .cleanedRecords(cleanedCount)
                .errors(allErrors)
                .warnings(allWarnings)
                .processingTimeMs(processingTime)
                .build();
                
        } catch (Exception e) {
            log.error("数据导入失败", e);
            
            List<String> errors = new ArrayList<>();
            errors.add("导入失败: " + e.getMessage());
            
            return responseBuilder
                .success(false)
                .message("数据导入失败")
                .successRecords(0)
                .failedRecords(0)
                .duplicateRecords(0)
                .cleanedRecords(0)
                .errors(errors)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
        }
    }
    
    /**
     * 检查是否为重复数据
     */
    private boolean isDuplicate(CropData cropData) {
        // 根据地区代码、作物名称、年份判断是否重复
        List<CropData> existingData = cropDataRepository.findByRegionCodeAndCropNameAndYear(
            cropData.getRegionCode(), 
            cropData.getCropName(), 
            cropData.getYear()
        );
        
        return !existingData.isEmpty();
    }
    
    /**
     * 批量导入数据（用于大文件）
     */
    @Transactional
    public DataImportResponse batchImportData(MultipartFile file, DataImportRequest request) {
        long startTime = System.currentTimeMillis();
        
        DataImportResponse.DataImportResponseBuilder responseBuilder = DataImportResponse.builder()
            .importTime(LocalDateTime.now())
            .fileName(file.getOriginalFilename())
            .fileSize(file.getSize())
            .errors(new ArrayList<>())
            .warnings(new ArrayList<>());
        
        try {
            // 解析文件
            List<CropData> parsedData = fileParsingService.parseFile(file);
            responseBuilder.totalRecords(parsedData.size());
            
            if (parsedData.isEmpty()) {
                return responseBuilder
                    .success(false)
                    .message("文件中没有有效数据")
                    .successRecords(0)
                    .failedRecords(0)
                    .duplicateRecords(0)
                    .cleanedRecords(0)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            }
            
            // 分批处理数据
            int batchSize = 1000; // 每批处理1000条记录
            int totalSuccess = 0;
            int totalFailed = 0;
            int totalDuplicate = 0;
            int totalCleaned = 0;
            List<String> allErrors = new ArrayList<>();
            List<String> allWarnings = new ArrayList<>();
            
            for (int i = 0; i < parsedData.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, parsedData.size());
                List<CropData> batch = parsedData.subList(i, endIndex);
                
                log.info("处理批次 {}-{}/{}", i + 1, endIndex, parsedData.size());
                
                // 处理当前批次
                DataImportResponse batchResult = processBatch(batch, request, i + 2); // +2 因为从第2行开始
                
                totalSuccess += batchResult.getSuccessRecords();
                totalFailed += batchResult.getFailedRecords();
                totalDuplicate += batchResult.getDuplicateRecords();
                totalCleaned += batchResult.getCleanedRecords();
                
                if (batchResult.getErrors() != null) {
                    allErrors.addAll(batchResult.getErrors());
                }
                if (batchResult.getWarnings() != null) {
                    allWarnings.addAll(batchResult.getWarnings());
                }
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            return responseBuilder
                .success(true)
                .message("批量数据导入完成")
                .successRecords(totalSuccess)
                .failedRecords(totalFailed)
                .duplicateRecords(totalDuplicate)
                .cleanedRecords(totalCleaned)
                .errors(allErrors)
                .warnings(allWarnings)
                .processingTimeMs(processingTime)
                .build();
                
        } catch (Exception e) {
            log.error("批量数据导入失败", e);
            
            List<String> errors = new ArrayList<>();
            errors.add("批量导入失败: " + e.getMessage());
            
            return responseBuilder
                .success(false)
                .message("批量数据导入失败")
                .successRecords(0)
                .failedRecords(0)
                .duplicateRecords(0)
                .cleanedRecords(0)
                .errors(errors)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
        }
    }
    
    /**
     * 处理单个批次
     */
    private DataImportResponse processBatch(List<CropData> batch, DataImportRequest request, int startRowNumber) {
        List<CropData> validData = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int cleanedCount = 0;
        
        // 数据清洗和验证
        if (request.getCleanData() || request.getValidateData()) {
            for (int i = 0; i < batch.size(); i++) {
                CropData cropData = batch.get(i);
                List<DataValidationResult> validationResults = 
                    dataCleaningService.cleanCropData(cropData, startRowNumber + i);
                
                boolean hasErrors = false;
                for (DataValidationResult result : validationResults) {
                    if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                        hasErrors = true;
                        errors.addAll(result.getErrors().stream()
                            .map(error -> "第" + result.getRowNumber() + "行 " + result.getFieldName() + ": " + error)
                            .collect(Collectors.toList()));
                    }
                    
                    if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
                        warnings.addAll(result.getWarnings().stream()
                            .map(warning -> "第" + result.getRowNumber() + "行 " + result.getFieldName() + ": " + warning)
                            .collect(Collectors.toList()));
                        
                        if (result.getCleanedValue() != null) {
                            cleanedCount++;
                        }
                    }
                }
                
                if (!hasErrors) {
                    validData.add(cropData);
                }
            }
        } else {
            validData = batch;
        }
        
        // 检查重复数据
        List<CropData> uniqueData = new ArrayList<>();
        int duplicateCount = 0;
        
        for (CropData cropData : validData) {
            if (!isDuplicate(cropData)) {
                uniqueData.add(cropData);
            } else {
                duplicateCount++;
                warnings.add("发现重复数据: " + cropData.getRegionName() + " - " + 
                           cropData.getCropName() + " - " + cropData.getYear());
            }
        }
        
        // 保存数据
        List<CropData> savedData = cropDataRepository.saveAll(uniqueData);
        
        return DataImportResponse.builder()
            .successRecords(savedData.size())
            .failedRecords(batch.size() - validData.size())
            .duplicateRecords(duplicateCount)
            .cleanedRecords(cleanedCount)
            .errors(errors)
            .warnings(warnings)
            .build();
    }
    
    /**
     * 验证文件格式
     */
    public boolean isValidFileFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }
        
        String extension = fileName.toLowerCase();
        return extension.endsWith(".csv") || extension.endsWith(".xlsx") || extension.endsWith(".xls");
    }
    
    /**
     * 获取支持的文件格式
     */
    public List<String> getSupportedFormats() {
        return List.of("CSV", "XLS", "XLSX");
    }
    
    /**
     * 获取文件大小限制（字节）
     */
    public long getMaxFileSize() {
        return 50 * 1024 * 1024; // 50MB
    }
}
