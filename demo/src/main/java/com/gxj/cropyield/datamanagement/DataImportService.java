package com.gxj.cropyield.datamanagement;

import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.yielddata.YieldRecordResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DataImportService {

    private static final int MIN_YEAR = 1978;
    private static final int MAX_YEAR = 2100;
    private static final int PREVIEW_LIMIT = 10;
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy年M月d日")
    };
    private static final Map<String, String> HEADER_MAPPING = createHeaderMapping();

    private final CropRepository cropRepository;
    private final RegionRepository regionRepository;
    private final YieldRecordRepository yieldRecordRepository;

    public DataImportService(
            CropRepository cropRepository,
            RegionRepository regionRepository,
            YieldRecordRepository yieldRecordRepository
    ) {
        this.cropRepository = cropRepository;
        this.regionRepository = regionRepository;
        this.yieldRecordRepository = yieldRecordRepository;
    }

    @Transactional
    public DataImportResponse importData(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传包含数据的文件");
        }

        List<ParsedRecord> parsedRecords = parseFile(file);
        if (parsedRecords.isEmpty()) {
            return new DataImportResponse(0, 0, 0, 0, List.of("文件中没有识别到有效数据行"), List.of());
        }

        int inserted = 0;
        int updated = 0;
        int processed = 0;
        List<String> warnings = new ArrayList<>();
        List<YieldRecordResponse> preview = new ArrayList<>();

        for (ParsedRecord parsed : parsedRecords) {
            Optional<CleanRecord> cleanedOptional = cleanRecord(parsed, warnings);
            if (cleanedOptional.isEmpty()) {
                continue;
            }
            CleanRecord cleaned = cleanedOptional.get();
            Crop crop = resolveCrop(cleaned, warnings);
            if (crop == null) {
                warnings.add("第" + cleaned.rowNumber() + "行：未能识别作物信息，已跳过");
                continue;
            }
            Region region = resolveRegion(cleaned, warnings);
            if (region == null) {
                warnings.add("第" + cleaned.rowNumber() + "行：未能识别地区信息，已跳过");
                continue;
            }

            YieldRecord entity = yieldRecordRepository
                    .findByCropIdAndRegionIdAndYear(crop.getId(), region.getId(), cleaned.year())
                    .orElseGet(YieldRecord::new);

            boolean isNew = entity.getId() == null;
            entity.setCrop(crop);
            entity.setRegion(region);
            entity.setYear(cleaned.year());
            entity.setSownArea(cleaned.sownArea());
            entity.setProduction(cleaned.production());
            entity.setYieldPerHectare(cleaned.yieldPerHectare());
            entity.setAveragePrice(cleaned.averagePrice());
            entity.setDataSource(cleaned.dataSource());
            entity.setCollectedAt(cleaned.collectedAt());

            YieldRecord saved = yieldRecordRepository.save(entity);
            if (isNew) {
                inserted++;
            } else {
                updated++;
            }
            processed++;

            if (preview.size() < PREVIEW_LIMIT) {
                preview.add(mapToResponse(saved));
            }
        }

        int skipped = parsedRecords.size() - processed;
        return new DataImportResponse(parsedRecords.size(), inserted, updated, skipped, warnings, preview);
    }

    private List<ParsedRecord> parseFile(MultipartFile file) {
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        try {
            if (lowerFilename.endsWith(".csv")) {
                return parseCsv(file);
            }
            if (lowerFilename.endsWith(".xls") || lowerFilename.endsWith(".xlsx")) {
                return parseExcel(file);
            }
            String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
            if (contentType.contains("csv")) {
                return parseCsv(file);
            }
            if (contentType.contains("excel") || contentType.contains("spreadsheet")) {
                return parseExcel(file);
            }
            throw new IllegalArgumentException("暂不支持的文件类型，请上传 CSV 或 Excel 文件");
        } catch (IOException exception) {
            throw new IllegalArgumentException("读取导入文件失败", exception);
        }
    }

    private List<ParsedRecord> parseCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setSkipHeaderRecord(true)
                     .setHeader()
                     .build()
                     .parse(reader)) {

            List<String> headers = parser.getHeaderNames();
            Map<String, String> normalizedHeaders = new LinkedHashMap<>();
            for (String header : headers) {
                String normalized = normalizeHeader(header);
                if (normalized != null) {
                    normalizedHeaders.put(header, normalized);
                }
            }

            List<ParsedRecord> records = new ArrayList<>();
            for (CSVRecord csvRecord : parser) {
                Map<String, String> valueMap = new HashMap<>();
                for (Map.Entry<String, String> entry : normalizedHeaders.entrySet()) {
                    valueMap.put(entry.getValue(), trimToNull(csvRecord.get(entry.getKey())));
                }
                if (valueMap.values().stream().allMatch(this::isBlank)) {
                    continue;
                }
                int rowNumber = (int) csvRecord.getRecordNumber() + 1;
                records.add(toParsedRecord(rowNumber, valueMap));
            }
            return records;
        }
    }

    private List<ParsedRecord> parseExcel(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                return List.of();
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }

            Map<Integer, String> headerMapping = new LinkedHashMap<>();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (Cell cell : headerRow) {
                String header = getCellString(cell, evaluator);
                String normalized = normalizeHeader(header);
                if (normalized != null) {
                    headerMapping.put(cell.getColumnIndex(), normalized);
                }
            }

            List<ParsedRecord> records = new ArrayList<>();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> valueMap = new HashMap<>();
                for (Map.Entry<Integer, String> entry : headerMapping.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());
                    valueMap.put(entry.getValue(), trimToNull(getCellString(cell, evaluator)));
                }
                if (valueMap.values().stream().allMatch(this::isBlank)) {
                    continue;
                }
                records.add(toParsedRecord(rowIndex + 1, valueMap));
            }
            return records;
        }
    }

    private String getCellString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = evaluator.evaluateFormulaCell(cell);
        }
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            }
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private ParsedRecord toParsedRecord(int rowNumber, Map<String, String> valueMap) {
        return new ParsedRecord(
                rowNumber,
                valueMap.get("cropName"),
                valueMap.get("cropCategory"),
                valueMap.get("cropDescription"),
                valueMap.get("regionName"),
                valueMap.get("regionLevel"),
                valueMap.get("regionParentName"),
                valueMap.get("regionDescription"),
                valueMap.get("year"),
                valueMap.get("sownArea"),
                valueMap.get("production"),
                valueMap.get("yieldPerHectare"),
                valueMap.get("averagePrice"),
                valueMap.get("dataSource"),
                valueMap.get("collectedAt")
        );
    }

    private Optional<CleanRecord> cleanRecord(ParsedRecord record, List<String> warnings) {
        String cropName = trimToNull(record.cropName());
        if (cropName == null) {
            warnings.add("第" + record.rowNumber() + "行：缺少作物名称，已跳过");
            return Optional.empty();
        }
        String regionName = trimToNull(record.regionName());
        if (regionName == null) {
            warnings.add("第" + record.rowNumber() + "行：缺少地区名称，已跳过");
            return Optional.empty();
        }
        Integer year = parseYear(record.year());
        if (year == null || year < MIN_YEAR || year > MAX_YEAR) {
            warnings.add("第" + record.rowNumber() + "行：年份不在支持范围内，已跳过");
            return Optional.empty();
        }
        Double production = parseDouble(record.production());
        if (production == null || production <= 0) {
            warnings.add("第" + record.rowNumber() + "行：产量缺失或非法，已跳过");
            return Optional.empty();
        }
        Double sownArea = parseDouble(record.sownArea());
        if (sownArea != null && sownArea < 0) {
            warnings.add("第" + record.rowNumber() + "行：播种面积出现负值，已置空");
            sownArea = null;
        }
        Double yieldPerHectare = parseDouble(record.yieldPerHectare());
        if ((yieldPerHectare == null || yieldPerHectare <= 0) && sownArea != null && sownArea > 0) {
            yieldPerHectare = production / sownArea;
        }
        if (yieldPerHectare != null && (yieldPerHectare < 0.5 || yieldPerHectare > 15)) {
            warnings.add(String.format("第%d行：单产值 %.2f 疑似异常，已保留供人工复核", record.rowNumber(), yieldPerHectare));
        }
        Double averagePrice = parseDouble(record.averagePrice());
        if (averagePrice != null && averagePrice < 0) {
            warnings.add("第" + record.rowNumber() + "行：价格出现负值，已置空");
            averagePrice = null;
        }
        LocalDate collectedAt = parseDate(record.collectedAt());

        return Optional.of(new CleanRecord(
                record.rowNumber(),
                cropName,
                trimToNull(record.cropCategory()),
                trimToNull(record.cropDescription()),
                regionName,
                normalizeRegionLevel(record.regionLevel()),
                trimToNull(record.regionParentName()),
                trimToNull(record.regionDescription()),
                year,
                round(sownArea),
                round(production),
                round(yieldPerHectare),
                round(averagePrice),
                trimToNull(record.dataSource()),
                collectedAt
        ));
    }

    private Crop resolveCrop(CleanRecord record, List<String> warnings) {
        return cropRepository.findByNameIgnoreCase(record.cropName())
                .orElseGet(() -> {
                    String category = Optional.ofNullable(record.cropCategory()).orElse("未分类");
                    String description = Optional.ofNullable(record.cropDescription()).orElse("由数据导入创建");
                    warnings.add("第" + record.rowNumber() + "行：新增作物“" + record.cropName() + "”已写入基础库");
                    Crop crop = new Crop();
                    crop.setCode(generateUniqueCropCode(record.cropName()));
                    crop.setName(record.cropName());
                    crop.setCategory(category);
                    crop.setDescription(description);
                    return cropRepository.save(crop);
                });
    }

    private Region resolveRegion(CleanRecord record, List<String> warnings) {
        return regionRepository.findByNameIgnoreCase(record.regionName())
                .orElseGet(() -> {
                    String level = Optional.ofNullable(record.regionLevel()).orElse("PREFECTURE");
                    String description = Optional.ofNullable(record.regionDescription()).orElse("由数据导入创建");
                    warnings.add("第" + record.rowNumber() + "行：新增地区“" + record.regionName() + "”已写入基础库");
                    Region region = new Region();
                    region.setCode(generateUniqueRegionCode(record.regionName(), record.regionParentName()));
                    region.setName(record.regionName());
                    region.setLevel(level);
                    region.setParentCode(resolveParentCode(record.regionParentName()));
                    region.setParentName(record.regionParentName());
                    region.setDescription(description);
                    return regionRepository.save(region);
                });
    }

    private String resolveParentCode(String parentName) {
        return Optional.ofNullable(trimToNull(parentName))
                .flatMap(regionRepository::findByNameIgnoreCase)
                .map(Region::getCode)
                .orElse(null);
    }

    private String generateUniqueCropCode(String name) {
        String base = slugify(name);
        String candidate = base;
        int counter = 1;
        while (cropRepository.findByCode(candidate).isPresent()) {
            candidate = base + "_" + counter++;
        }
        return candidate;
    }

    private String generateUniqueRegionCode(String name, String parentName) {
        String base = slugify(name);
        String prefix = Optional.ofNullable(resolveParentCode(parentName))
                .map(code -> code + "-")
                .orElse("");
        String candidate = prefix + base;
        int counter = 1;
        while (regionRepository.findByCode(candidate).isPresent()) {
            candidate = prefix + base + "-" + counter++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String trimmed = Optional.ofNullable(trimToNull(value)).orElse("DATA");
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String alphanumeric = normalized.replaceAll("[^A-Za-z0-9]", "");
        if (alphanumeric.isEmpty()) {
            alphanumeric = Integer.toHexString(trimmed.hashCode());
        }
        String upper = alphanumeric.toUpperCase(Locale.ROOT);
        if (upper.isEmpty()) {
            upper = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        }
        return upper;
    }

    private YieldRecordResponse mapToResponse(YieldRecord record) {
        double revenue = 0.0;
        if (record.getProduction() != null && record.getAveragePrice() != null) {
            revenue = record.getProduction() * record.getAveragePrice() * 0.1;
        }
        return new YieldRecordResponse(
                record.getId(),
                record.getCrop().getName(),
                record.getCrop().getCategory(),
                record.getRegion().getName(),
                record.getRegion().getLevel(),
                record.getYear(),
                record.getSownArea(),
                record.getProduction(),
                record.getYieldPerHectare(),
                record.getAveragePrice(),
                revenue,
                record.getDataSource(),
                record.getCollectedAt()
        );
    }

    private Integer parseYear(String value) {
        Double numeric = parseDouble(value);
        if (numeric == null) {
            return null;
        }
        return (int) Math.round(numeric);
    }

    private Double parseDouble(String raw) {
        String numeric = cleanNumericString(raw);
        if (numeric == null || numeric.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(numeric);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String cleanNumericString(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().replaceAll("[ ,，]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.replaceAll("[^0-9+\\-.,]", "");
    }

    private LocalDate parseDate(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        Integer year = parseYear(trimmed);
        if (year != null && year >= MIN_YEAR && year <= MAX_YEAR) {
            return LocalDate.of(year, 12, 31);
        }
        return null;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return null;
        }
        String trimmed = header.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT).replaceAll("[\\s_]", "");
        String mapped = HEADER_MAPPING.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        return HEADER_MAPPING.getOrDefault(trimmed, null);
    }

    private static Map<String, String> createHeaderMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("crop", "cropName");
        mapping.put("cropname", "cropName");
        mapping.put("作物", "cropName");
        mapping.put("作物名称", "cropName");
        mapping.put("cropcategory", "cropCategory");
        mapping.put("作物类别", "cropCategory");
        mapping.put("cropdescription", "cropDescription");
        mapping.put("作物描述", "cropDescription");
        mapping.put("region", "regionName");
        mapping.put("regionname", "regionName");
        mapping.put("地区", "regionName");
        mapping.put("地区名称", "regionName");
        mapping.put("regionlevel", "regionLevel");
        mapping.put("地区级别", "regionLevel");
        mapping.put("级别", "regionLevel");
        mapping.put("parentregion", "regionParentName");
        mapping.put("parentname", "regionParentName");
        mapping.put("上级地区", "regionParentName");
        mapping.put("地区描述", "regionDescription");
        mapping.put("regiondescription", "regionDescription");
        mapping.put("year", "year");
        mapping.put("年份", "year");
        mapping.put("sownarea", "sownArea");
        mapping.put("播种面积", "sownArea");
        mapping.put("production", "production");
        mapping.put("产量", "production");
        mapping.put("yield", "yieldPerHectare");
        mapping.put("yieldperhectare", "yieldPerHectare");
        mapping.put("单产", "yieldPerHectare");
        mapping.put("averageprice", "averagePrice");
        mapping.put("平均价格", "averagePrice");
        mapping.put("price", "averagePrice");
        mapping.put("datasource", "dataSource");
        mapping.put("数据来源", "dataSource");
        mapping.put("source", "dataSource");
        mapping.put("collectedat", "collectedAt");
        mapping.put("采集日期", "collectedAt");
        mapping.put("收集日期", "collectedAt");
        return mapping;
    }

    private String normalizeRegionLevel(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (trimmed.contains("省") || upper.contains("PROVINCE")) {
            return "PROVINCE";
        }
        if (trimmed.contains("州") || trimmed.contains("市") || upper.contains("PREFECTURE")) {
            return "PREFECTURE";
        }
        if (trimmed.contains("县") || trimmed.contains("区") || upper.contains("COUNTY")) {
            return "COUNTY";
        }
        if (trimmed.contains("乡") || trimmed.contains("镇") || trimmed.contains("村") || upper.contains("TOWNSHIP")) {
            return "TOWNSHIP";
        }
        return upper;
    }

    private Double round(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ParsedRecord(
            int rowNumber,
            String cropName,
            String cropCategory,
            String cropDescription,
            String regionName,
            String regionLevel,
            String regionParentName,
            String regionDescription,
            String year,
            String sownArea,
            String production,
            String yieldPerHectare,
            String averagePrice,
            String dataSource,
            String collectedAt
    ) {
    }

    private record CleanRecord(
            int rowNumber,
            String cropName,
            String cropCategory,
            String cropDescription,
            String regionName,
            String regionLevel,
            String regionParentName,
            String regionDescription,
            Integer year,
            Double sownArea,
            Double production,
            Double yieldPerHectare,
            Double averagePrice,
            String dataSource,
            LocalDate collectedAt
    ) {
    }
}
