package nongye.example.demo.service;

import nongye.example.demo.entity.CropData;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileParsingService {
    
    /**
     * 解析上传的文件
     */
    public List<CropData> parseFile(MultipartFile file) throws IOException, CsvException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        String fileExtension = getFileExtension(fileName).toLowerCase();
        
        switch (fileExtension) {
            case "csv":
                return parseCsvFile(file);
            case "xlsx":
            case "xls":
                return parseExcelFile(file);
            default:
                throw new IllegalArgumentException("不支持的文件格式：" + fileExtension + "。支持的格式：CSV, XLS, XLSX");
        }
    }
    
    /**
     * 解析CSV文件
     */
    private List<CropData> parseCsvFile(MultipartFile file) throws IOException, CsvException {
        List<CropData> cropDataList = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> records = reader.readAll();
            
            if (records.isEmpty()) {
                throw new IllegalArgumentException("CSV文件为空");
            }
            
            // 跳过标题行
            boolean isFirstRow = true;
            int rowNumber = 0;
            
            for (String[] record : records) {
                rowNumber++;
                
                if (isFirstRow) {
                    isFirstRow = false;
                    validateCsvHeaders(record);
                    continue;
                }
                
                try {
                    CropData cropData = parseCsvRecord(record, rowNumber);
                    if (cropData != null) {
                        cropDataList.add(cropData);
                    }
                } catch (Exception e) {
                    log.warn("解析CSV第{}行时出错: {}", rowNumber, e.getMessage());
                    // 继续处理其他行
                }
            }
        }
        
        return cropDataList;
    }
    
    /**
     * 解析Excel文件
     */
    private List<CropData> parseExcelFile(MultipartFile file) throws IOException {
        List<CropData> cropDataList = new ArrayList<>();
        
        Workbook workbook = null;
        try {
            String fileName = file.getOriginalFilename();
            if (fileName != null && fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(file.getInputStream());
            } else {
                workbook = new HSSFWorkbook(file.getInputStream());
            }
            
            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表
            
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("Excel文件为空");
            }
            
            // 验证标题行
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                validateExcelHeaders(headerRow);
            }
            
            // 解析数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    CropData cropData = parseExcelRow(row, i + 1);
                    if (cropData != null) {
                        cropDataList.add(cropData);
                    }
                } catch (Exception e) {
                    log.warn("解析Excel第{}行时出错: {}", i + 1, e.getMessage());
                    // 继续处理其他行
                }
            }
            
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        
        return cropDataList;
    }
    
    /**
     * 验证CSV标题行
     */
    private void validateCsvHeaders(String[] headers) {
        if (headers.length < 4) { // 至少需要基本字段
            throw new IllegalArgumentException("CSV文件缺少必要的列。至少需要：地区代码、地区名称、作物类型、作物名称");
        }
        
        // 检查必要的列是否存在
        boolean hasRegionCode = false, hasRegionName = false, hasCropType = false, hasCropName = false;
        
        for (String header : headers) {
            if (header != null) {
                String trimmedHeader = header.trim();
                if (trimmedHeader.equals("地区代码") || trimmedHeader.equals("regionCode")) {
                    hasRegionCode = true;
                } else if (trimmedHeader.equals("地区名称") || trimmedHeader.equals("regionName")) {
                    hasRegionName = true;
                } else if (trimmedHeader.equals("作物类型") || trimmedHeader.equals("cropType")) {
                    hasCropType = true;
                } else if (trimmedHeader.equals("作物名称") || trimmedHeader.equals("cropName")) {
                    hasCropName = true;
                }
            }
        }
        
        if (!hasRegionCode || !hasRegionName || !hasCropType || !hasCropName) {
            throw new IllegalArgumentException("CSV文件缺少必要的列：地区代码、地区名称、作物类型、作物名称");
        }
    }
    
    /**
     * 验证Excel标题行
     */
    private void validateExcelHeaders(Row headerRow) {
        if (headerRow.getPhysicalNumberOfCells() < 4) {
            throw new IllegalArgumentException("Excel文件缺少必要的列。至少需要：地区代码、地区名称、作物类型、作物名称");
        }
        
        // 检查必要的列是否存在
        boolean hasRegionCode = false, hasRegionName = false, hasCropType = false, hasCropName = false;
        
        for (Cell cell : headerRow) {
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String header = cell.getStringCellValue().trim();
                if (header.equals("地区代码") || header.equals("regionCode")) {
                    hasRegionCode = true;
                } else if (header.equals("地区名称") || header.equals("regionName")) {
                    hasRegionName = true;
                } else if (header.equals("作物类型") || header.equals("cropType")) {
                    hasCropType = true;
                } else if (header.equals("作物名称") || header.equals("cropName")) {
                    hasCropName = true;
                }
            }
        }
        
        if (!hasRegionCode || !hasRegionName || !hasCropType || !hasCropName) {
            throw new IllegalArgumentException("Excel文件缺少必要的列：地区代码、地区名称、作物类型、作物名称");
        }
    }
    
    /**
     * 解析CSV记录
     */
    private CropData parseCsvRecord(String[] record, int rowNumber) {
        if (record.length < 4) {
            log.warn("第{}行数据不完整，跳过", rowNumber);
            return null;
        }
        
        CropData cropData = new CropData();
        cropData.setCreatedAt(LocalDateTime.now());
        
        try {
            // 基本字段
            cropData.setRegionCode(parseString(record, 0));
            cropData.setRegionName(parseString(record, 1));
            cropData.setCropType(parseString(record, 2));
            cropData.setCropName(parseString(record, 3));
            
            // 数值字段
            if (record.length > 4) cropData.setPlantingArea(parseBigDecimal(record, 4));
            if (record.length > 5) cropData.setYield(parseBigDecimal(record, 5));
            if (record.length > 6) cropData.setUnitYield(parseBigDecimal(record, 6));
            if (record.length > 7) cropData.setPrice(parseBigDecimal(record, 7));
            if (record.length > 8) cropData.setTotalValue(parseBigDecimal(record, 8));
            if (record.length > 9) cropData.setYear(parseInteger(record, 9));
            if (record.length > 10) cropData.setSeason(parseString(record, 10));
            if (record.length > 11) cropData.setWeatherCondition(parseString(record, 11));
            if (record.length > 12) cropData.setPestDiseaseLevel(parseString(record, 12));
            if (record.length > 13) cropData.setFertilizerUsage(parseBigDecimal(record, 13));
            if (record.length > 14) cropData.setPesticideUsage(parseBigDecimal(record, 14));
            if (record.length > 15) cropData.setIrrigationArea(parseBigDecimal(record, 15));
            if (record.length > 16) cropData.setDataSource(parseString(record, 16));
            
            return cropData;
            
        } catch (Exception e) {
            log.error("解析第{}行数据时出错: {}", rowNumber, e.getMessage());
            throw new RuntimeException("第" + rowNumber + "行数据解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析Excel行
     */
    private CropData parseExcelRow(Row row, int rowNumber) {
        CropData cropData = new CropData();
        cropData.setCreatedAt(LocalDateTime.now());
        
        try {
            // 基本字段
            cropData.setRegionCode(getCellStringValue(row, 0));
            cropData.setRegionName(getCellStringValue(row, 1));
            cropData.setCropType(getCellStringValue(row, 2));
            cropData.setCropName(getCellStringValue(row, 3));
            
            // 数值字段
            cropData.setPlantingArea(getCellBigDecimalValue(row, 4));
            cropData.setYield(getCellBigDecimalValue(row, 5));
            cropData.setUnitYield(getCellBigDecimalValue(row, 6));
            cropData.setPrice(getCellBigDecimalValue(row, 7));
            cropData.setTotalValue(getCellBigDecimalValue(row, 8));
            cropData.setYear(getCellIntegerValue(row, 9));
            cropData.setSeason(getCellStringValue(row, 10));
            cropData.setWeatherCondition(getCellStringValue(row, 11));
            cropData.setPestDiseaseLevel(getCellStringValue(row, 12));
            cropData.setFertilizerUsage(getCellBigDecimalValue(row, 13));
            cropData.setPesticideUsage(getCellBigDecimalValue(row, 14));
            cropData.setIrrigationArea(getCellBigDecimalValue(row, 15));
            cropData.setDataSource(getCellStringValue(row, 16));
            
            return cropData;
            
        } catch (Exception e) {
            log.error("解析第{}行数据时出错: {}", rowNumber, e.getMessage());
            throw new RuntimeException("第" + rowNumber + "行数据解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析字符串字段
     */
    private String parseString(String[] record, int index) {
        if (index >= record.length) return null;
        String value = record[index];
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }
    
    /**
     * 解析BigDecimal字段
     */
    private BigDecimal parseBigDecimal(String[] record, int index) {
        if (index >= record.length) return null;
        String value = record[index];
        if (value == null || value.trim().isEmpty()) return null;
        
        try {
            // 移除可能的千分位分隔符
            value = value.trim().replace(",", "").replace("，", "");
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("无法解析数值: {}", value);
            return null;
        }
    }
    
    /**
     * 解析Integer字段
     */
    private Integer parseInteger(String[] record, int index) {
        if (index >= record.length) return null;
        String value = record[index];
        if (value == null || value.trim().isEmpty()) return null;
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("无法解析整数: {}", value);
            return null;
        }
    }
    
    /**
     * 获取Excel单元格字符串值
     */
    private String getCellStringValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                String value = cell.getStringCellValue();
                return (value == null || value.trim().isEmpty()) ? null : value.trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 将数值转换为字符串，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return null;
        }
    }
    
    /**
     * 获取Excel单元格BigDecimal值
     */
    private BigDecimal getCellBigDecimalValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                String value = cell.getStringCellValue();
                if (value == null || value.trim().isEmpty()) return null;
                try {
                    // 移除可能的千分位分隔符
                    value = value.trim().replace(",", "").replace("，", "");
                    return new BigDecimal(value);
                } catch (NumberFormatException e) {
                    log.warn("无法解析数值: {}", value);
                    return null;
                }
            case FORMULA:
                try {
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    log.warn("无法解析公式结果为数值");
                    return null;
                }
            default:
                return null;
        }
    }
    
    /**
     * 获取Excel单元格Integer值
     */
    private Integer getCellIntegerValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                String value = cell.getStringCellValue();
                if (value == null || value.trim().isEmpty()) return null;
                try {
                    return Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    log.warn("无法解析整数: {}", value);
                    return null;
                }
            case FORMULA:
                try {
                    return (int) cell.getNumericCellValue();
                } catch (Exception e) {
                    log.warn("无法解析公式结果为整数");
                    return null;
                }
            default:
                return null;
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
    
    /**
     * 生成CSV模板
     */
    public String generateCsvTemplate() {
        return "地区代码,地区名称,作物类型,作物名称,种植面积,产量,单产,价格,总产值,年份,季节,天气条件,病虫害程度,化肥使用量,农药使用量,灌溉面积,数据来源\n" +
               "110000,北京市,粮食作物,小麦,1000.50,500.25,0.5,3000.00,1500750.00,2023,秋,晴朗,轻微,50.5,10.2,800.0,统计局\n" +
               "310000,上海市,蔬菜,白菜,500.00,800.00,1.6,2000.00,1600000.00,2023,冬,多云,无,30.0,5.5,450.0,农业部";
    }
}
