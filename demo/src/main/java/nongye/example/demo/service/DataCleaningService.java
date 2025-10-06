package nongye.example.demo.service;

import nongye.example.demo.entity.CropData;
import nongye.example.demo.dto.DataValidationResult;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DataCleaningService {
    
    // 地区代码正则表达式
    private static final Pattern REGION_CODE_PATTERN = Pattern.compile("^[0-9]{6}$");
    
    // 作物类型枚举
    private static final String[] VALID_CROP_TYPES = {
        "粮食作物", "经济作物", "蔬菜", "水果", "其他"
    };
    
    // 季节枚举
    private static final String[] VALID_SEASONS = {
        "春", "夏", "秋", "冬", "全年"
    };
    
    // 天气条件枚举
    private static final String[] VALID_WEATHER_CONDITIONS = {
        "晴朗", "多云", "阴天", "小雨", "中雨", "大雨", "暴雨", "干旱", "洪涝", "冰雹", "霜冻"
    };
    
    // 病虫害程度枚举
    private static final String[] VALID_PEST_LEVELS = {
        "无", "轻微", "中等", "严重", "极严重"
    };
    
    /**
     * 清洗作物数据
     */
    public List<DataValidationResult> cleanCropData(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        
        // 清洗地区代码
        results.addAll(cleanRegionCode(cropData, rowNumber));
        
        // 清洗地区名称
        results.addAll(cleanRegionName(cropData, rowNumber));
        
        // 清洗作物类型
        results.addAll(cleanCropType(cropData, rowNumber));
        
        // 清洗作物名称
        results.addAll(cleanCropName(cropData, rowNumber));
        
        // 清洗数值字段
        results.addAll(cleanNumericFields(cropData, rowNumber));
        
        // 清洗年份
        results.addAll(cleanYear(cropData, rowNumber));
        
        // 清洗季节
        results.addAll(cleanSeason(cropData, rowNumber));
        
        // 清洗天气条件
        results.addAll(cleanWeatherCondition(cropData, rowNumber));
        
        // 清洗病虫害程度
        results.addAll(cleanPestDiseaseLevel(cropData, rowNumber));
        
        // 清洗数据来源
        results.addAll(cleanDataSource(cropData, rowNumber));
        
        // 计算衍生字段
        results.addAll(calculateDerivedFields(cropData, rowNumber));
        
        return results;
    }
    
    /**
     * 清洗地区代码
     */
    private List<DataValidationResult> cleanRegionCode(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        String originalValue = cropData.getRegionCode();
        
        if (originalValue == null || originalValue.trim().isEmpty()) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("地区代码不能为空"))
                .rowNumber(rowNumber)
                .fieldName("regionCode")
                .originalValue(originalValue)
                .validationType("REQUIRED")
                .build());
            return results;
        }
        
        String cleanedValue = originalValue.trim();
        
        // 如果不是6位数字，尝试补零或截取
        if (!REGION_CODE_PATTERN.matcher(cleanedValue).matches()) {
            if (cleanedValue.matches("^[0-9]+$")) {
                if (cleanedValue.length() < 6) {
                    cleanedValue = String.format("%06d", Integer.parseInt(cleanedValue));
                } else if (cleanedValue.length() > 6) {
                    cleanedValue = cleanedValue.substring(0, 6);
                }
                
                results.add(DataValidationResult.builder()
                    .isValid(true)
                    .warnings(List.of("地区代码已自动格式化为6位数字"))
                    .rowNumber(rowNumber)
                    .fieldName("regionCode")
                    .originalValue(originalValue)
                    .cleanedValue(cleanedValue)
                    .validationType("FORMAT")
                    .build());
            } else {
                results.add(DataValidationResult.builder()
                    .isValid(false)
                    .errors(List.of("地区代码必须为6位数字"))
                    .rowNumber(rowNumber)
                    .fieldName("regionCode")
                    .originalValue(originalValue)
                    .validationType("FORMAT")
                    .build());
                return results;
            }
        }
        
        cropData.setRegionCode(cleanedValue);
        return results;
    }
    
    /**
     * 清洗地区名称
     */
    private List<DataValidationResult> cleanRegionName(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        String originalValue = cropData.getRegionName();
        
        if (originalValue == null || originalValue.trim().isEmpty()) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("地区名称不能为空"))
                .rowNumber(rowNumber)
                .fieldName("regionName")
                .originalValue(originalValue)
                .validationType("REQUIRED")
                .build());
            return results;
        }
        
        String cleanedValue = originalValue.trim();
        
        // 移除特殊字符，只保留中文、英文、数字和常见标点
        cleanedValue = cleanedValue.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s\\-_()]", "");
        
        if (!cleanedValue.equals(originalValue.trim())) {
            results.add(DataValidationResult.builder()
                .isValid(true)
                .warnings(List.of("地区名称已清洗，移除了特殊字符"))
                .rowNumber(rowNumber)
                .fieldName("regionName")
                .originalValue(originalValue)
                .cleanedValue(cleanedValue)
                .validationType("CLEAN")
                .build());
        }
        
        cropData.setRegionName(cleanedValue);
        return results;
    }
    
    /**
     * 清洗作物类型
     */
    private List<DataValidationResult> cleanCropType(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        String originalValue = cropData.getCropType();
        
        if (originalValue == null || originalValue.trim().isEmpty()) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("作物类型不能为空"))
                .rowNumber(rowNumber)
                .fieldName("cropType")
                .originalValue(originalValue)
                .validationType("REQUIRED")
                .build());
            return results;
        }
        
        String cleanedValue = originalValue.trim();
        
        // 标准化作物类型
        cleanedValue = standardizeCropType(cleanedValue);
        
        // 验证是否为有效的作物类型
        boolean isValid = false;
        for (String validType : VALID_CROP_TYPES) {
            if (validType.equals(cleanedValue)) {
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("无效的作物类型，有效值为：" + String.join("、", VALID_CROP_TYPES)))
                .rowNumber(rowNumber)
                .fieldName("cropType")
                .originalValue(originalValue)
                .validationType("ENUM")
                .build());
            return results;
        }
        
        if (!cleanedValue.equals(originalValue.trim())) {
            results.add(DataValidationResult.builder()
                .isValid(true)
                .warnings(List.of("作物类型已标准化"))
                .rowNumber(rowNumber)
                .fieldName("cropType")
                .originalValue(originalValue)
                .cleanedValue(cleanedValue)
                .validationType("STANDARDIZE")
                .build());
        }
        
        cropData.setCropType(cleanedValue);
        return results;
    }
    
    /**
     * 清洗作物名称
     */
    private List<DataValidationResult> cleanCropName(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        String originalValue = cropData.getCropName();
        
        if (originalValue == null || originalValue.trim().isEmpty()) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("作物名称不能为空"))
                .rowNumber(rowNumber)
                .fieldName("cropName")
                .originalValue(originalValue)
                .validationType("REQUIRED")
                .build());
            return results;
        }
        
        String cleanedValue = originalValue.trim();
        
        // 移除特殊字符
        cleanedValue = cleanedValue.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s\\-_()]", "");
        
        if (!cleanedValue.equals(originalValue.trim())) {
            results.add(DataValidationResult.builder()
                .isValid(true)
                .warnings(List.of("作物名称已清洗，移除了特殊字符"))
                .rowNumber(rowNumber)
                .fieldName("cropName")
                .originalValue(originalValue)
                .cleanedValue(cleanedValue)
                .validationType("CLEAN")
                .build());
        }
        
        cropData.setCropName(cleanedValue);
        return results;
    }
    
    /**
     * 清洗数值字段
     */
    private List<DataValidationResult> cleanNumericFields(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        
        // 清洗种植面积
        results.addAll(cleanBigDecimalField(cropData.getPlantingArea(), "plantingArea", 
            "种植面积", rowNumber, BigDecimal.ZERO, new BigDecimal("1000000"), 2));
        
        // 清洗产量
        results.addAll(cleanBigDecimalField(cropData.getYield(), "yield", 
            "产量", rowNumber, BigDecimal.ZERO, new BigDecimal("100000"), 2));
        
        // 清洗单产
        results.addAll(cleanBigDecimalField(cropData.getUnitYield(), "unitYield", 
            "单产", rowNumber, BigDecimal.ZERO, new BigDecimal("100"), 4));
        
        // 清洗价格
        results.addAll(cleanBigDecimalField(cropData.getPrice(), "price", 
            "价格", rowNumber, BigDecimal.ZERO, new BigDecimal("100000"), 2));
        
        // 清洗总产值
        results.addAll(cleanBigDecimalField(cropData.getTotalValue(), "totalValue", 
            "总产值", rowNumber, BigDecimal.ZERO, new BigDecimal("10000000000"), 2));
        
        // 清洗化肥使用量
        results.addAll(cleanBigDecimalField(cropData.getFertilizerUsage(), "fertilizerUsage", 
            "化肥使用量", rowNumber, BigDecimal.ZERO, new BigDecimal("10000"), 3));
        
        // 清洗农药使用量
        results.addAll(cleanBigDecimalField(cropData.getPesticideUsage(), "pesticideUsage", 
            "农药使用量", rowNumber, BigDecimal.ZERO, new BigDecimal("1000"), 3));
        
        // 清洗灌溉面积
        results.addAll(cleanBigDecimalField(cropData.getIrrigationArea(), "irrigationArea", 
            "灌溉面积", rowNumber, BigDecimal.ZERO, new BigDecimal("1000000"), 2));
        
        return results;
    }
    
    /**
     * 清洗BigDecimal字段
     */
    private List<DataValidationResult> cleanBigDecimalField(BigDecimal value, String fieldName, 
            String fieldDisplayName, int rowNumber, BigDecimal minValue, BigDecimal maxValue, int scale) {
        List<DataValidationResult> results = new ArrayList<>();
        
        if (value == null) {
            return results; // 允许为空
        }
        
        BigDecimal originalValue = value;
        BigDecimal cleanedValue = value;
        
        // 检查范围
        if (cleanedValue.compareTo(minValue) < 0) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of(fieldDisplayName + "不能小于" + minValue))
                .rowNumber(rowNumber)
                .fieldName(fieldName)
                .originalValue(originalValue)
                .validationType("RANGE")
                .build());
            return results;
        }
        
        if (cleanedValue.compareTo(maxValue) > 0) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of(fieldDisplayName + "不能大于" + maxValue))
                .rowNumber(rowNumber)
                .fieldName(fieldName)
                .originalValue(originalValue)
                .validationType("RANGE")
                .build());
            return results;
        }
        
        // 调整精度
        if (cleanedValue.scale() > scale) {
            cleanedValue = cleanedValue.setScale(scale, RoundingMode.HALF_UP);
            results.add(DataValidationResult.builder()
                .isValid(true)
                .warnings(List.of(fieldDisplayName + "精度已调整为" + scale + "位小数"))
                .rowNumber(rowNumber)
                .fieldName(fieldName)
                .originalValue(originalValue)
                .cleanedValue(cleanedValue)
                .validationType("PRECISION")
                .build());
        }
        
        return results;
    }
    
    /**
     * 清洗年份
     */
    private List<DataValidationResult> cleanYear(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        Integer originalValue = cropData.getYear();
        
        if (originalValue == null) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("年份不能为空"))
                .rowNumber(rowNumber)
                .fieldName("year")
                .originalValue(originalValue)
                .validationType("REQUIRED")
                .build());
            return results;
        }
        
        int currentYear = java.time.Year.now().getValue();
        
        if (originalValue < 1900 || originalValue > currentYear + 1) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("年份必须在1900年到" + (currentYear + 1) + "年之间"))
                .rowNumber(rowNumber)
                .fieldName("year")
                .originalValue(originalValue)
                .validationType("RANGE")
                .build());
        }
        
        return results;
    }
    
    /**
     * 清洗季节
     */
    private List<DataValidationResult> cleanSeason(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        String originalValue = cropData.getSeason();
        
        if (originalValue == null || originalValue.trim().isEmpty()) {
            return results; // 季节可以为空
        }
        
        String cleanedValue = originalValue.trim();
        
        // 标准化季节
        cleanedValue = standardizeSeason(cleanedValue);
        
        // 验证是否为有效的季节
        boolean isValid = false;
        for (String validSeason : VALID_SEASONS) {
            if (validSeason.equals(cleanedValue)) {
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("无效的季节，有效值为：" + String.join("、", VALID_SEASONS)))
                .rowNumber(rowNumber)
                .fieldName("season")
                .originalValue(originalValue)
                .validationType("ENUM")
                .build());
            return results;
        }
        
        if (!cleanedValue.equals(originalValue.trim())) {
            results.add(DataValidationResult.builder()
                .isValid(true)
                .warnings(List.of("季节已标准化"))
                .rowNumber(rowNumber)
                .fieldName("season")
                .originalValue(originalValue)
                .cleanedValue(cleanedValue)
                .validationType("STANDARDIZE")
                .build());
        }
        
        cropData.setSeason(cleanedValue);
        return results;
    }
    
    /**
     * 清洗天气条件
     */
    private List<DataValidationResult> cleanWeatherCondition(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        String originalValue = cropData.getWeatherCondition();
        
        if (originalValue == null || originalValue.trim().isEmpty()) {
            return results; // 天气条件可以为空
        }
        
        String cleanedValue = originalValue.trim();
        
        // 标准化天气条件
        cleanedValue = standardizeWeatherCondition(cleanedValue);
        
        // 验证是否为有效的天气条件
        boolean isValid = false;
        for (String validCondition : VALID_WEATHER_CONDITIONS) {
            if (validCondition.equals(cleanedValue)) {
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("无效的天气条件，有效值为：" + String.join("、", VALID_WEATHER_CONDITIONS)))
                .rowNumber(rowNumber)
                .fieldName("weatherCondition")
                .originalValue(originalValue)
                .validationType("ENUM")
                .build());
            return results;
        }
        
        if (!cleanedValue.equals(originalValue.trim())) {
            results.add(DataValidationResult.builder()
                .isValid(true)
                .warnings(List.of("天气条件已标准化"))
                .rowNumber(rowNumber)
                .fieldName("weatherCondition")
                .originalValue(originalValue)
                .cleanedValue(cleanedValue)
                .validationType("STANDARDIZE")
                .build());
        }
        
        cropData.setWeatherCondition(cleanedValue);
        return results;
    }
    
    /**
     * 清洗病虫害程度
     */
    private List<DataValidationResult> cleanPestDiseaseLevel(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        String originalValue = cropData.getPestDiseaseLevel();
        
        if (originalValue == null || originalValue.trim().isEmpty()) {
            return results; // 病虫害程度可以为空
        }
        
        String cleanedValue = originalValue.trim();
        
        // 标准化病虫害程度
        cleanedValue = standardizePestLevel(cleanedValue);
        
        // 验证是否为有效的病虫害程度
        boolean isValid = false;
        for (String validLevel : VALID_PEST_LEVELS) {
            if (validLevel.equals(cleanedValue)) {
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            results.add(DataValidationResult.builder()
                .isValid(false)
                .errors(List.of("无效的病虫害程度，有效值为：" + String.join("、", VALID_PEST_LEVELS)))
                .rowNumber(rowNumber)
                .fieldName("pestDiseaseLevel")
                .originalValue(originalValue)
                .validationType("ENUM")
                .build());
            return results;
        }
        
        if (!cleanedValue.equals(originalValue.trim())) {
            results.add(DataValidationResult.builder()
                .isValid(true)
                .warnings(List.of("病虫害程度已标准化"))
                .rowNumber(rowNumber)
                .fieldName("pestDiseaseLevel")
                .originalValue(originalValue)
                .cleanedValue(cleanedValue)
                .validationType("STANDARDIZE")
                .build());
        }
        
        cropData.setPestDiseaseLevel(cleanedValue);
        return results;
    }
    
    /**
     * 清洗数据来源
     */
    private List<DataValidationResult> cleanDataSource(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        String originalValue = cropData.getDataSource();
        
        if (originalValue == null || originalValue.trim().isEmpty()) {
            return results; // 数据来源可以为空
        }
        
        String cleanedValue = originalValue.trim();
        
        // 移除特殊字符
        cleanedValue = cleanedValue.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s\\-_().]", "");
        
        if (!cleanedValue.equals(originalValue.trim())) {
            results.add(DataValidationResult.builder()
                .isValid(true)
                .warnings(List.of("数据来源已清洗，移除了特殊字符"))
                .rowNumber(rowNumber)
                .fieldName("dataSource")
                .originalValue(originalValue)
                .cleanedValue(cleanedValue)
                .validationType("CLEAN")
                .build());
        }
        
        cropData.setDataSource(cleanedValue);
        return results;
    }
    
    /**
     * 计算衍生字段
     */
    private List<DataValidationResult> calculateDerivedFields(CropData cropData, int rowNumber) {
        List<DataValidationResult> results = new ArrayList<>();
        
        // 计算单产（产量/种植面积）
        if (cropData.getYield() != null && cropData.getPlantingArea() != null 
                && cropData.getPlantingArea().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal calculatedUnitYield = cropData.getYield()
                .divide(cropData.getPlantingArea(), 4, RoundingMode.HALF_UP);
            
            if (cropData.getUnitYield() == null) {
                cropData.setUnitYield(calculatedUnitYield);
                results.add(DataValidationResult.builder()
                    .isValid(true)
                    .warnings(List.of("单产已自动计算"))
                    .rowNumber(rowNumber)
                    .fieldName("unitYield")
                    .cleanedValue(calculatedUnitYield)
                    .validationType("CALCULATE")
                    .build());
            } else {
                // 检查计算值与输入值的差异
                BigDecimal diff = calculatedUnitYield.subtract(cropData.getUnitYield()).abs();
                BigDecimal tolerance = calculatedUnitYield.multiply(new BigDecimal("0.05")); // 5%容差
                
                if (diff.compareTo(tolerance) > 0) {
                    results.add(DataValidationResult.builder()
                        .isValid(true)
                        .warnings(List.of("单产与计算值差异较大，请检查数据"))
                        .rowNumber(rowNumber)
                        .fieldName("unitYield")
                        .originalValue(cropData.getUnitYield())
                        .cleanedValue(calculatedUnitYield)
                        .validationType("CONSISTENCY")
                        .build());
                }
            }
        }
        
        // 计算总产值（产量*价格）
        if (cropData.getYield() != null && cropData.getPrice() != null) {
            BigDecimal calculatedTotalValue = cropData.getYield()
                .multiply(cropData.getPrice()).setScale(2, RoundingMode.HALF_UP);
            
            if (cropData.getTotalValue() == null) {
                cropData.setTotalValue(calculatedTotalValue);
                results.add(DataValidationResult.builder()
                    .isValid(true)
                    .warnings(List.of("总产值已自动计算"))
                    .rowNumber(rowNumber)
                    .fieldName("totalValue")
                    .cleanedValue(calculatedTotalValue)
                    .validationType("CALCULATE")
                    .build());
            } else {
                // 检查计算值与输入值的差异
                BigDecimal diff = calculatedTotalValue.subtract(cropData.getTotalValue()).abs();
                BigDecimal tolerance = calculatedTotalValue.multiply(new BigDecimal("0.05")); // 5%容差
                
                if (diff.compareTo(tolerance) > 0) {
                    results.add(DataValidationResult.builder()
                        .isValid(true)
                        .warnings(List.of("总产值与计算值差异较大，请检查数据"))
                        .rowNumber(rowNumber)
                        .fieldName("totalValue")
                        .originalValue(cropData.getTotalValue())
                        .cleanedValue(calculatedTotalValue)
                        .validationType("CONSISTENCY")
                        .build());
                }
            }
        }
        
        return results;
    }
    
    /**
     * 标准化作物类型
     */
    private String standardizeCropType(String cropType) {
        if (cropType == null) return null;
        
        String normalized = cropType.trim().toLowerCase();
        
        // 粮食作物
        if (normalized.contains("粮食") || normalized.contains("谷物") || 
            normalized.contains("水稻") || normalized.contains("小麦") || 
            normalized.contains("玉米") || normalized.contains("大豆")) {
            return "粮食作物";
        }
        
        // 经济作物
        if (normalized.contains("经济") || normalized.contains("棉花") || 
            normalized.contains("油菜") || normalized.contains("甘蔗") || 
            normalized.contains("烟草")) {
            return "经济作物";
        }
        
        // 蔬菜
        if (normalized.contains("蔬菜") || normalized.contains("菜") || 
            normalized.contains("白菜") || normalized.contains("萝卜") || 
            normalized.contains("土豆") || normalized.contains("番茄")) {
            return "蔬菜";
        }
        
        // 水果
        if (normalized.contains("水果") || normalized.contains("果") || 
            normalized.contains("苹果") || normalized.contains("梨") || 
            normalized.contains("桃") || normalized.contains("葡萄")) {
            return "水果";
        }
        
        return "其他";
    }
    
    /**
     * 标准化季节
     */
    private String standardizeSeason(String season) {
        if (season == null) return null;
        
        String normalized = season.trim().toLowerCase();
        
        if (normalized.contains("春") || normalized.equals("spring")) {
            return "春";
        } else if (normalized.contains("夏") || normalized.equals("summer")) {
            return "夏";
        } else if (normalized.contains("秋") || normalized.equals("autumn") || normalized.equals("fall")) {
            return "秋";
        } else if (normalized.contains("冬") || normalized.equals("winter")) {
            return "冬";
        } else if (normalized.contains("全年") || normalized.contains("年") || normalized.equals("year")) {
            return "全年";
        }
        
        return season.trim();
    }
    
    /**
     * 标准化天气条件
     */
    private String standardizeWeatherCondition(String condition) {
        if (condition == null) return null;
        
        String normalized = condition.trim().toLowerCase();
        
        if (normalized.contains("晴") || normalized.contains("sunny")) {
            return "晴朗";
        } else if (normalized.contains("云") || normalized.contains("cloudy")) {
            return "多云";
        } else if (normalized.contains("阴") || normalized.contains("overcast")) {
            return "阴天";
        } else if (normalized.contains("小雨") || normalized.contains("light rain")) {
            return "小雨";
        } else if (normalized.contains("中雨") || normalized.contains("moderate rain")) {
            return "中雨";
        } else if (normalized.contains("大雨") || normalized.contains("heavy rain")) {
            return "大雨";
        } else if (normalized.contains("暴雨") || normalized.contains("storm")) {
            return "暴雨";
        } else if (normalized.contains("干旱") || normalized.contains("drought")) {
            return "干旱";
        } else if (normalized.contains("洪") || normalized.contains("flood")) {
            return "洪涝";
        } else if (normalized.contains("雹") || normalized.contains("hail")) {
            return "冰雹";
        } else if (normalized.contains("霜") || normalized.contains("frost")) {
            return "霜冻";
        }
        
        return condition.trim();
    }
    
    /**
     * 标准化病虫害程度
     */
    private String standardizePestLevel(String level) {
        if (level == null) return null;
        
        String normalized = level.trim().toLowerCase();
        
        if (normalized.contains("无") || normalized.contains("none") || normalized.equals("0")) {
            return "无";
        } else if (normalized.contains("轻") || normalized.contains("light") || normalized.equals("1")) {
            return "轻微";
        } else if (normalized.contains("中") || normalized.contains("moderate") || normalized.equals("2")) {
            return "中等";
        } else if (normalized.contains("严重") && !normalized.contains("极")) {
            return "严重";
        } else if (normalized.contains("极") || normalized.contains("severe") || normalized.equals("4")) {
            return "极严重";
        } else if (normalized.contains("重") || normalized.equals("3")) {
            return "严重";
        }
        
        return level.trim();
    }
}
