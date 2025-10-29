package com.gxj.cropyield.datamanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxj.cropyield.datamanagement.dto.DataImportErrorView;
import com.gxj.cropyield.datamanagement.dto.DataImportJobDetailView;
import com.gxj.cropyield.datamanagement.dto.DataImportJobPageResponse;
import com.gxj.cropyield.datamanagement.dto.DataImportJobView;
import com.gxj.cropyield.datamanagement.model.DataImportJob;
import com.gxj.cropyield.datamanagement.model.DataImportJobError;
import com.gxj.cropyield.datamanagement.model.DataImportJobStatus;
import com.gxj.cropyield.datamanagement.repository.DataImportJobRepository;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.dto.YieldRecordResponse;
import com.gxj.cropyield.modules.dataset.dto.WeatherRecordImportPreview;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile.DatasetType;
import com.gxj.cropyield.modules.dataset.repository.DatasetFileRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
/**
 * 数据导入模块的业务接口，定义数据导入相关的核心业务操作。
 * <p>核心方法：deleteTasks、submitImport、mapToView、listJobs、getJobDetail、processJobAsync、ensureDatasetFile、updateDatasetFileMetadata。</p>
 */

@Service
public class DataImportService {

    private static final int MIN_YEAR = 1978;
    private static final int PREVIEW_LIMIT = 10;
    private static final int MAX_WARNING_STORE = 50;
    private static final int MAX_ERROR_STORE = 50;
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy年M月d日")
    };
    private static final Pattern WEATHER_DATE_TOKEN = Pattern.compile("\\d{4}(?:[-/.年]\\d{1,2})(?:[-/.月]\\d{1,2})");
    private static final Pattern WEATHER_DATE_COMPONENTS = Pattern.compile("(\\d{4})\\D*(\\d{1,2})\\D*(\\d{1,2})");
    private static final Map<String, List<String>> HEADER_SYNONYMS = createHeaderSynonyms();
    private static final JaroWinklerSimilarity SIMILARITY = new JaroWinklerSimilarity();
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d+(\\.\\d+)?");

    private final CropRepository cropRepository;
    private final RegionRepository regionRepository;
    private final DatasetFileRepository datasetFileRepository;
    private final DataImportJobRepository jobRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DataImportService(CropRepository cropRepository,
                             RegionRepository regionRepository,
                             DatasetFileRepository datasetFileRepository,
                             DataImportJobRepository jobRepository,
                             JdbcTemplate jdbcTemplate,
                             ObjectMapper objectMapper) {
        this.cropRepository = cropRepository;
        this.regionRepository = regionRepository;
        this.datasetFileRepository = datasetFileRepository;
        this.jobRepository = jobRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int deleteTasks(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return 0;
        }
        List<DataImportJob> jobs = jobRepository.findByTaskIdIn(taskIds);
        if (jobs.isEmpty()) {
            return 0;
        }
        jobRepository.deleteAll(jobs);
        return jobs.size();
    }

    public DataImportJobView submitImport(MultipartFile file, DatasetType datasetType, String datasetName, String datasetDescription) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传包含数据的文件");
        }
        String safeDatasetName = Optional.ofNullable(datasetName)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseGet(() -> extractBaseName(file.getOriginalFilename()));
        if (safeDatasetName.length() > 128) {
            safeDatasetName = safeDatasetName.substring(0, 128);
        }
        Path storageRoot = Paths.get("uploads", "datasets");
        Path savedPath = persistFile(storageRoot, safeDatasetName, file);

        DataImportJob job = new DataImportJob();
        job.setTaskId(UUID.randomUUID().toString());
        job.setDatasetName(safeDatasetName);
        job.setDatasetDescription(Optional.ofNullable(datasetDescription)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(value -> value.length() > 256 ? value.substring(0, 256) : value)
                .orElse(null));
        job.setDatasetType(Optional.ofNullable(datasetType).orElse(DatasetType.YIELD));
        job.setOriginalFilename(file.getOriginalFilename());
        job.setStoragePath(savedPath.toString());
        job.setStatus(DataImportJobStatus.QUEUED);
        job.setMessage("任务已创建，等待异步执行");
        jobRepository.save(job);

        processJobAsync(job.getId());

        return mapToView(job);
    }

    public DataImportJobPageResponse listJobs(String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Specification<DataImportJob> specification = buildSpecification(status, keyword);
        Page<DataImportJob> jobPage = jobRepository.findAll(specification, pageable);
        List<DataImportJobView> views = jobPage.getContent().stream()
                .map(this::mapToView)
                .toList();
        return new DataImportJobPageResponse(
                views,
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages()
        );
    }

    public DataImportJobDetailView getJobDetail(String taskId) {
        DataImportJob job = jobRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("未找到对应的导入任务"));
        List<String> warnings = readPayload(job.getWarningsPayload());
        List<?> preview = readPreview(job.getPreviewPayload(), job.getDatasetType());
        List<DataImportErrorView> errors = job.getErrors().stream()
                .sorted(Comparator.comparing(DataImportJobError::getRowNumber, Comparator.nullsLast(Integer::compareTo)))
                .limit(20)
                .map(error -> new DataImportErrorView(
                        Optional.ofNullable(error.getRowNumber()).orElse(-1),
                        error.getErrorCode(),
                        error.getMessage(),
                        error.getRawValue()
                ))
                .toList();
        return new DataImportJobDetailView(mapToView(job), warnings, errors, preview);
    }

    @Async("importTaskExecutor")
    @Transactional
    public void processJobAsync(Long jobId) {
        DataImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("未找到导入任务"));
        job.setStatus(DataImportJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        job.setMessage("正在解析导入文件");
        jobRepository.save(job);

        try {
            Path filePath = Paths.get(job.getStoragePath());
            if (!Files.exists(filePath)) {
                throw new IllegalStateException("导入文件不存在或已被移除");
            }
            List<ParsedRecord> parsedRecords = parseFile(filePath);
            job.setTotalRows(parsedRecords.size());
            job.setMessage("正在进行数据校验与清洗");
            jobRepository.save(job);

            ImportResult result = switch (job.getDatasetType()) {
                case WEATHER -> processWeatherRecords(job, parsedRecords);
                default -> processYieldRecords(job, parsedRecords);
            };

            job.clearErrors();
            result.errors().stream().limit(MAX_ERROR_STORE).forEach(job::addError);
            job.setProcessedRows(result.inserted() + result.updated());
            job.setInsertedRows(result.inserted());
            job.setUpdatedRows(result.updated());
            job.setFailedRows(result.failed());
            job.setSkippedRows(result.skipped());
            job.setWarningCount(result.warnings().size());
            job.setWarningsPayload(writePayload(limitList(result.warnings(), MAX_WARNING_STORE)));
            job.setPreviewPayload(writePreview(result.preview()));
            job.setFinishedAt(LocalDateTime.now());
            job.setStatus(DataImportJobStatus.SUCCEEDED);
            job.setMessage(buildCompletionMessage(job));
            jobRepository.save(job);

            if (result.datasetFile() != null) {
                updateDatasetFileMetadata(result.datasetFile(), job);
            }
        } catch (Exception exception) {
            job.setStatus(DataImportJobStatus.FAILED);
            job.setFinishedAt(LocalDateTime.now());
            job.setMessage("导入失败：" + exception.getMessage());
            jobRepository.save(job);
        }
    }

    private DatasetFile ensureDatasetFile(DataImportJob job) {
        return datasetFileRepository.findByName(job.getDatasetName())
                .map(existing -> {
                    existing.setType(job.getDatasetType());
                    existing.setStoragePath(job.getStoragePath());
                    if (job.getDatasetDescription() != null && !job.getDatasetDescription().isBlank()) {
                        existing.setDescription(job.getDatasetDescription());
                    }
                    return datasetFileRepository.save(existing);
                })
                .orElseGet(() -> {
                    DatasetFile created = new DatasetFile();
                    created.setName(job.getDatasetName());
                    created.setType(job.getDatasetType());
                    created.setStoragePath(job.getStoragePath());
                    created.setDescription(job.getDatasetDescription());
                    return datasetFileRepository.save(created);
                });
    }

    private void updateDatasetFileMetadata(DatasetFile datasetFile, DataImportJob job) {
        datasetFile.setType(job.getDatasetType());
        datasetFile.setStoragePath(job.getStoragePath());
        datasetFile.setDescription(buildDatasetDescription(job));
        datasetFileRepository.save(datasetFile);
    }

    private ImportResult processYieldRecords(DataImportJob job, List<ParsedRecord> parsedRecords) {
        List<String> warnings = new ArrayList<>();
        List<DataImportJobError> errorEntities = new ArrayList<>();
        List<ValidRecord> validRecords = new ArrayList<>();
        List<YieldRecordResponse> preview = new ArrayList<>();

        Map<String, Crop> cropCache = new HashMap<>();
        Map<String, Region> regionCache = new HashMap<>();
        AtomicInteger failedCounter = new AtomicInteger();

        for (ParsedRecord record : parsedRecords) {
            Optional<ValidRecord> validated = validateYieldRecord(record, warnings, errorEntities, cropCache, regionCache, failedCounter);
            if (validated.isPresent()) {
                ValidRecord valid = validated.get();
                validRecords.add(valid);
                if (preview.size() < PREVIEW_LIMIT) {
                    preview.add(toYieldPreview(valid));
                }
            }
        }

        DatasetFile datasetFile = null;
        int inserted = 0;
        int updated = 0;
        if (!validRecords.isEmpty()) {
            datasetFile = ensureDatasetFile(job);
            if (datasetFile != null) {
                job.setDatasetFileId(datasetFile.getId());
            }
            job.setMessage("正在批量写入数据库");
            jobRepository.save(job);
            UpsertResult result = upsertYieldRecords(validRecords, datasetFile);
            inserted = result.inserted();
            updated = result.updated();
        }

        int processed = inserted + updated;
        int failedRows = failedCounter.get();
        int skippedRows = Math.max(job.getTotalRows() - processed - failedRows, 0);

        return new ImportResult(inserted, updated, failedRows, skippedRows, warnings, errorEntities, new ArrayList<>(preview), datasetFile);
    }

    private ImportResult processWeatherRecords(DataImportJob job, List<ParsedRecord> parsedRecords) {
        List<String> warnings = new ArrayList<>();
        List<DataImportJobError> errorEntities = new ArrayList<>();
        List<ValidWeatherRecord> validRecords = new ArrayList<>();
        List<WeatherRecordImportPreview> preview = new ArrayList<>();

        Map<String, Region> regionCache = new HashMap<>();
        AtomicInteger failedCounter = new AtomicInteger();

        for (ParsedRecord record : parsedRecords) {
            Optional<ValidWeatherRecord> validated = validateWeatherRecord(record, warnings, errorEntities, regionCache, failedCounter);
            if (validated.isPresent()) {
                ValidWeatherRecord valid = validated.get();
                validRecords.add(valid);
                if (preview.size() < PREVIEW_LIMIT) {
                    preview.add(toWeatherPreview(valid));
                }
            }
        }

        DatasetFile datasetFile = null;
        int inserted = 0;
        int updated = 0;
        if (!validRecords.isEmpty()) {
            datasetFile = ensureDatasetFile(job);
            if (datasetFile != null) {
                job.setDatasetFileId(datasetFile.getId());
            }
            job.setMessage("正在批量写入数据库");
            jobRepository.save(job);
            UpsertResult result = upsertWeatherRecords(validRecords, datasetFile);
            inserted = result.inserted();
            updated = result.updated();
        }

        int processed = inserted + updated;
        int failedRows = failedCounter.get();
        int skippedRows = Math.max(job.getTotalRows() - processed - failedRows, 0);

        return new ImportResult(inserted, updated, failedRows, skippedRows, warnings, errorEntities, new ArrayList<>(preview), datasetFile);
    }

    private UpsertResult upsertYieldRecords(List<ValidRecord> records, DatasetFile datasetFile) {
        int batchSize = 500;
        int inserted = 0;
        int updated = 0;
        for (int i = 0; i < records.size(); i += batchSize) {
            List<ValidRecord> batch = records.subList(i, Math.min(i + batchSize, records.size()));
            Set<YieldRowKey> existing = fetchExistingYieldKeys(batch);
            String sql = """
                    INSERT INTO dataset_yield_record (dataset_file_id, crop_id, region_id, year, sown_area, production, yield_per_hectare, average_price, data_source, collected_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    ON DUPLICATE KEY UPDATE
                        dataset_file_id = VALUES(dataset_file_id),
                        sown_area = VALUES(sown_area),
                        production = VALUES(production),
                        yield_per_hectare = VALUES(yield_per_hectare),
                        average_price = VALUES(average_price),
                        data_source = VALUES(data_source),
                        collected_at = VALUES(collected_at),
                        updated_at = NOW()
                    """;
            try {
                jdbcTemplate.batchUpdate(sql, batch, batch.size(), (preparedStatement, record) -> {
                    preparedStatement.setLong(1, datasetFile.getId());
                    preparedStatement.setLong(2, record.crop().getId());
                    preparedStatement.setLong(3, record.region().getId());
                    preparedStatement.setInt(4, record.year());
                    setNullableDouble(preparedStatement, 5, record.sownArea());
                    setNullableDouble(preparedStatement, 6, record.production());
                    setNullableDouble(preparedStatement, 7, record.yieldPerHectare());
                    setNullableDouble(preparedStatement, 8, record.averagePrice());
                    preparedStatement.setString(9, record.dataSource());
                    if (record.collectedAt() != null) {
                        preparedStatement.setObject(10, java.sql.Date.valueOf(record.collectedAt()));
                    } else {
                        preparedStatement.setObject(10, null);
                    }
                });
            } catch (DataAccessException exception) {
                throw new IllegalStateException("写入数据库失败", exception);
            }

            for (ValidRecord record : batch) {
                YieldRowKey key = new YieldRowKey(record.crop().getId(), record.region().getId(), record.year());
                if (existing.contains(key)) {
                    updated++;
                } else {
                    inserted++;
                }
            }
        }
        return new UpsertResult(inserted, updated);
    }

    private UpsertResult upsertWeatherRecords(List<ValidWeatherRecord> records, DatasetFile datasetFile) {
        int batchSize = 500;
        int inserted = 0;
        int updated = 0;
        for (int i = 0; i < records.size(); i += batchSize) {
            List<ValidWeatherRecord> batch = records.subList(i, Math.min(i + batchSize, records.size()));
            Set<WeatherRowKey> existing = fetchExistingWeatherKeys(batch);
            String sql = """
                    INSERT INTO dataset_weather_record (dataset_file_id, region_id, record_date, max_temperature, min_temperature, weather_text, wind, sunshine_hours, data_source, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                    ON DUPLICATE KEY UPDATE
                        dataset_file_id = VALUES(dataset_file_id),
                        max_temperature = VALUES(max_temperature),
                        min_temperature = VALUES(min_temperature),
                        weather_text = VALUES(weather_text),
                        wind = VALUES(wind),
                        sunshine_hours = VALUES(sunshine_hours),
                        data_source = VALUES(data_source),
                        updated_at = NOW()
                    """;
            try {
                jdbcTemplate.batchUpdate(sql, batch, batch.size(), (preparedStatement, record) -> {
                    if (datasetFile != null) {
                        preparedStatement.setLong(1, datasetFile.getId());
                    } else {
                        preparedStatement.setObject(1, null);
                    }
                    preparedStatement.setLong(2, record.region().getId());
                    preparedStatement.setObject(3, java.sql.Date.valueOf(record.recordDate()));
                    setNullableDouble(preparedStatement, 4, record.maxTemperature());
                    setNullableDouble(preparedStatement, 5, record.minTemperature());
                    preparedStatement.setString(6, record.weatherText());
                    preparedStatement.setString(7, record.wind());
                    setNullableDouble(preparedStatement, 8, record.sunshineHours());
                    preparedStatement.setString(9, record.dataSource());
                });
            } catch (DataAccessException exception) {
                throw new IllegalStateException("写入数据库失败", exception);
            }

            for (ValidWeatherRecord record : batch) {
                WeatherRowKey key = new WeatherRowKey(record.region().getId(), record.recordDate());
                if (existing.contains(key)) {
                    updated++;
                } else {
                    inserted++;
                }
            }
        }
        return new UpsertResult(inserted, updated);
    }

    private Set<YieldRowKey> fetchExistingYieldKeys(List<ValidRecord> batch) {
        if (batch.isEmpty()) {
            return Collections.emptySet();
        }
        String placeholders = batch.stream()
                .map(record -> "(?, ?, ?)")
                .collect(Collectors.joining(","));
        String sql = "SELECT crop_id, region_id, year FROM dataset_yield_record WHERE (crop_id, region_id, year) IN " + "(" + placeholders + ")";
        Object[] params = new Object[batch.size() * 3];
        int index = 0;
        for (ValidRecord record : batch) {
            params[index++] = record.crop().getId();
            params[index++] = record.region().getId();
            params[index++] = record.year();
        }
        List<YieldRowKey> keys = jdbcTemplate.query(sql, params, (resultSet, rowNum) -> new YieldRowKey(
                resultSet.getLong("crop_id"),
                resultSet.getLong("region_id"),
                resultSet.getInt("year")
        ));
        return new HashSet<>(keys);
    }

    private Set<WeatherRowKey> fetchExistingWeatherKeys(List<ValidWeatherRecord> batch) {
        if (batch.isEmpty()) {
            return Collections.emptySet();
        }
        String placeholders = batch.stream()
                .map(record -> "(?, ?)")
                .collect(Collectors.joining(","));
        String sql = "SELECT region_id, record_date FROM dataset_weather_record WHERE (region_id, record_date) IN (" + placeholders + ")";
        Object[] params = new Object[batch.size() * 2];
        int index = 0;
        for (ValidWeatherRecord record : batch) {
            params[index++] = record.region().getId();
            params[index++] = java.sql.Date.valueOf(record.recordDate());
        }
        List<WeatherRowKey> keys = jdbcTemplate.query(sql, params, (resultSet, rowNum) -> new WeatherRowKey(
                resultSet.getLong("region_id"),
                resultSet.getDate("record_date").toLocalDate()
        ));
        return new HashSet<>(keys);
    }

    private Optional<ValidRecord> validateYieldRecord(ParsedRecord record,
                                                      List<String> warnings,
                                                      List<DataImportJobError> errors,
                                                      Map<String, Crop> cropCache,
                                                      Map<String, Region> regionCache,
                                                      AtomicInteger failedCounter) {
        Map<String, String> values = record.values();
        Map<String, Function<Double, Double>> converters = record.converters();

        String cropName = trimToNull(values.get("cropName"));
        if (cropName == null) {
            recordError(errors, record.rowNumber(), "REQUIRED", "缺少作物信息", null);
            failedCounter.incrementAndGet();
            return Optional.empty();
        }

        String regionName = trimToNull(values.get("regionName"));
        if (regionName == null) {
            recordError(errors, record.rowNumber(), "REQUIRED", "缺少地区信息", null);
            failedCounter.incrementAndGet();
            return Optional.empty();
        }

        Integer year = parseYear(values.get("year"));
        int currentYear = LocalDate.now().getYear();
        if (year == null) {
            recordError(errors, record.rowNumber(), "YEAR", "年份不是有效数字", values.get("year"));
            failedCounter.incrementAndGet();
            return Optional.empty();
        }
        if (year < MIN_YEAR || year > currentYear) {
            recordError(errors, record.rowNumber(), "YEAR_RANGE", "年份不在允许范围内", String.valueOf(year));
            failedCounter.incrementAndGet();
            return Optional.empty();
        }

        Double production = parseNumeric(values.get("production"));
        Double sownArea = parseNumeric(values.get("sownArea"));
        Double yieldPerHectare = parseNumeric(values.get("yieldPerHectare"));
        Double averagePrice = parseNumeric(values.get("averagePrice"));

        if (production == null) {
            recordError(errors, record.rowNumber(), "PRODUCTION", "产量缺失或格式错误", values.get("production"));
            failedCounter.incrementAndGet();
            return Optional.empty();
        }

        production = applyConverter(converters.get("production"), production);
        sownArea = applyConverter(converters.get("sownArea"), sownArea);
        yieldPerHectare = applyConverter(converters.get("yieldPerHectare"), yieldPerHectare);

        if (sownArea != null && sownArea < 0) {
            warnings.add("第" + record.rowNumber() + "行：播种面积出现负值，已置空");
            sownArea = null;
        }
        if (production < 0) {
            recordError(errors, record.rowNumber(), "PRODUCTION_RANGE", "产量不能为负数", String.valueOf(production));
            failedCounter.incrementAndGet();
            return Optional.empty();
        }

        if ((yieldPerHectare == null || yieldPerHectare <= 0) && sownArea != null && sownArea > 0) {
            yieldPerHectare = production / sownArea;
        }
        if (yieldPerHectare != null && (yieldPerHectare < 0.3 || yieldPerHectare > 25)) {
            warnings.add(String.format(Locale.CHINA, "第%d行：单产值 %.2f 疑似异常", record.rowNumber(), yieldPerHectare));
        }

        if (averagePrice != null) {
            averagePrice = applyConverter(converters.get("averagePrice"), averagePrice);
            if (averagePrice < 0) {
                warnings.add("第" + record.rowNumber() + "行：平均价格出现负值，已置空");
                averagePrice = null;
            }
        }

        LocalDate collectedAt = parseDate(values.get("collectedAt"));

        Crop crop = resolveCrop(cropName, values.get("cropCategory"), values.get("cropDescription"), cropCache, warnings, record.rowNumber());
        Region region = resolveRegion(regionName, values.get("regionLevel"), values.get("regionParentName"), values.get("regionDescription"), regionCache, warnings, record.rowNumber());

        String dataSource = trimToNull(values.get("dataSource"));

        ValidRecord validRecord = new ValidRecord(record.rowNumber(), crop, region, year, round(sownArea), round(production), round(yieldPerHectare), round(averagePrice), dataSource, collectedAt);
        return Optional.of(validRecord);
    }

    private Optional<ValidWeatherRecord> validateWeatherRecord(ParsedRecord record,
                                                               List<String> warnings,
                                                               List<DataImportJobError> errors,
                                                               Map<String, Region> regionCache,
                                                               AtomicInteger failedCounter) {
        Map<String, String> values = record.values();

        String regionName = trimToNull(values.get("regionName"));
        if (regionName == null) {
            recordError(errors, record.rowNumber(), "REQUIRED", "缺少地区信息", null);
            failedCounter.incrementAndGet();
            return Optional.empty();
        }

        LocalDate recordDate = parseWeatherDate(values.get("recordDate"));
        if (recordDate == null) {
            recordError(errors, record.rowNumber(), "DATE", "日期不是有效格式（示例：2009-01-01）", values.get("recordDate"));
            failedCounter.incrementAndGet();
            return Optional.empty();
        }

        Double maxTemperature = parseTemperature(values.get("maxTemperature"));
        Double minTemperature = parseTemperature(values.get("minTemperature"));

        if (maxTemperature == null && minTemperature == null) {
            recordError(errors, record.rowNumber(), "TEMPERATURE", "最高温或最低温缺失", values.get("maxTemperature"));
            failedCounter.incrementAndGet();
            return Optional.empty();
        }

        if (maxTemperature != null && minTemperature != null && maxTemperature < minTemperature) {
            warnings.add(String.format(Locale.CHINA, "第%d行：最高温%.2f低于最低温%.2f，已自动互换", record.rowNumber(), maxTemperature, minTemperature));
            double temp = maxTemperature;
            maxTemperature = minTemperature;
            minTemperature = temp;
        }

        Double sunshineHours = parseSunshineHours(values.get("sunshineHours"));
        if (sunshineHours != null && sunshineHours < 0) {
            warnings.add("第" + record.rowNumber() + "行：日照时长出现负值，已置空");
            sunshineHours = null;
        }

        Region region = resolveRegion(regionName, null, null, null, regionCache, warnings, record.rowNumber());
        String weatherText = trimToNull(values.get("weatherText"));
        String wind = trimToNull(values.get("wind"));
        String dataSource = trimToNull(values.get("dataSource"));

        ValidWeatherRecord validRecord = new ValidWeatherRecord(
                record.rowNumber(),
                region,
                recordDate,
                round(maxTemperature),
                round(minTemperature),
                weatherText,
                wind,
                round(sunshineHours),
                dataSource
        );
        return Optional.of(validRecord);
    }

    private Crop resolveCrop(String cropName,
                             String cropCategory,
                             String cropDescription,
                             Map<String, Crop> cache,
                             List<String> warnings,
                             int rowNumber) {
        String key = normalizeKey(cropName);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Crop crop = cropRepository.findByNameIgnoreCase(cropName)
                .orElseGet(() -> {
                    Crop created = new Crop();
                    created.setCode(generateUniqueCropCode(cropName));
                    created.setName(cropName);
                    created.setCategory(Optional.ofNullable(trimToNull(cropCategory)).orElse("未分类"));
                    created.setDescription(Optional.ofNullable(trimToNull(cropDescription)).orElse("由导入任务自动创建"));
                    warnings.add("第" + rowNumber + "行：新增作物“" + cropName + "”已写入基础库");
                    return cropRepository.save(created);
                });
        cache.put(key, crop);
        return crop;
    }

    private Region resolveRegion(String regionName,
                                 String levelValue,
                                 String parentName,
                                 String description,
                                 Map<String, Region> cache,
                                 List<String> warnings,
                                 int rowNumber) {
        String key = normalizeKey(regionName);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        Region region = regionRepository.findByNameIgnoreCase(regionName)
                .orElseGet(() -> {
                    Region created = new Region();
                    created.setCode(generateUniqueRegionCode(regionName, parentName));
                    created.setName(regionName);
                    created.setLevel(Optional.ofNullable(normalizeRegionLevel(levelValue)).orElse("PREFECTURE"));
                    created.setParentName(trimToNull(parentName));
                    created.setParentCode(resolveParentCode(parentName));
                    created.setDescription(Optional.ofNullable(trimToNull(description)).orElse("由导入任务自动创建"));
                    warnings.add("第" + rowNumber + "行：新增地区“" + regionName + "”已写入基础库");
                    return regionRepository.save(created);
                });
        cache.put(key, region);
        return region;
    }

    private String resolveParentCode(String parentName) {
        return Optional.ofNullable(trimToNull(parentName))
                .flatMap(regionRepository::findByNameIgnoreCase)
                .map(Region::getCode)
                .orElse(null);
    }

    private YieldRecordResponse toYieldPreview(ValidRecord record) {
        Double revenue = null;
        if (record.production() != null && record.averagePrice() != null) {
            revenue = record.production() * record.averagePrice() * 0.1;
        }
        return new YieldRecordResponse(
                null,
                record.crop().getName(),
                record.crop().getCategory(),
                record.region().getName(),
                record.region().getLevel(),
                record.year(),
                record.sownArea(),
                record.production(),
                record.yieldPerHectare(),
                record.averagePrice(),
                revenue,
                record.dataSource(),
                record.collectedAt()
        );
    }

    private WeatherRecordImportPreview toWeatherPreview(ValidWeatherRecord record) {
        return new WeatherRecordImportPreview(
                record.region().getName(),
                record.recordDate(),
                record.maxTemperature(),
                record.minTemperature(),
                record.weatherText(),
                record.wind(),
                record.sunshineHours(),
                record.dataSource()
        );
    }

    private Specification<DataImportJob> buildSpecification(String status, String keyword) {
        Specification<DataImportJob> specification = Specification.where(null);
        if (status != null && !status.isBlank()) {
            try {
                DataImportJobStatus jobStatus = DataImportJobStatus.valueOf(status.toUpperCase(Locale.ROOT));
                specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), jobStatus));
            } catch (IllegalArgumentException ignored) {
                specification = specification.and((root, query, builder) -> builder.disjunction());
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            String likeValue = "%" + keyword.trim() + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(root.get("datasetName"), likeValue),
                    builder.like(root.get("originalFilename"), likeValue)
            ));
        }
        return specification;
    }

    private List<ParsedRecord> parseFile(Path path) throws IOException {
        String filename = Optional.ofNullable(path.getFileName()).map(Path::toString).orElse("").toLowerCase(Locale.ROOT);
        if (filename.endsWith(".csv")) {
            return parseCsv(path);
        }
        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            return parseExcel(path);
        }
        throw new IllegalArgumentException("暂不支持的文件类型，请上传 CSV 或 Excel 文件");
    }

    private List<ParsedRecord> parseCsv(Path path) throws IOException {
        CSVFormat format = buildCsvFormat(path);
        try (Reader reader = createUtf8Reader(path);
             CSVParser parser = format.parse(reader)) {
            List<String> headers = parser.getHeaderNames();
            List<String> sanitizedHeaders = headers.stream()
                    .map(this::sanitizeHeader)
                    .toList();
            HeaderMapping mapping = buildHeaderMapping(sanitizedHeaders);
            List<ParsedRecord> records = new ArrayList<>();
            for (CSVRecord csvRecord : parser) {
                Map<String, String> valueMap = new HashMap<>();
                for (int index = 0; index < sanitizedHeaders.size(); index++) {
                    ColumnMatch match = mapping.matches().get(index);
                    if (match == null) {
                        continue;
                    }
                    valueMap.put(match.canonicalName(), trimToNull(csvRecord.get(index)));
                }
                if (valueMap.values().stream().allMatch(this::isBlank)) {
                    continue;
                }
                records.add(new ParsedRecord((int) csvRecord.getRecordNumber() + 1, valueMap, mapping.converterMap()));
            }
            return records;
        } catch (MalformedInputException exception) {
            throw new IllegalArgumentException("CSV 文件不是 UTF-8 编码", exception);
        }
    }

    private CSVFormat buildCsvFormat(Path path) throws IOException {
        char delimiter = detectDelimiter(path);
        return CSVFormat.DEFAULT
                .builder()
                .setSkipHeaderRecord(false)
                .setHeader()
                .setDelimiter(delimiter)
                .build();
    }

    private char detectDelimiter(Path path) throws IOException {
        List<Character> candidates = List.of(',', '\t', ';', '|');
        Map<Character, Integer> counts = new HashMap<>();
        for (Character candidate : candidates) {
            counts.put(candidate, 0);
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String cleaned = line.replace("\uFEFF", "");
                for (Character candidate : candidates) {
                    counts.computeIfPresent(candidate, (key, value) -> value + countOccurrences(cleaned, candidate));
                }
                break;
            }
        }
        return counts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse(',');
    }

    private int countOccurrences(String line, char delimiter) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == delimiter) {
                count++;
            }
        }
        return count;
    }

    private List<ParsedRecord> parseExcel(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                return List.of();
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellString(cell, workbook.getCreationHelper().createFormulaEvaluator()));
            }
            HeaderMapping mapping = buildHeaderMapping(headers);

            List<ParsedRecord> records = new ArrayList<>();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> valueMap = new HashMap<>();
                for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                    ColumnMatch match = mapping.matches().get(columnIndex);
                    if (match == null) {
                        continue;
                    }
                    Cell cell = row.getCell(columnIndex);
                    valueMap.put(match.canonicalName(), trimToNull(getCellString(cell, evaluator)));
                }
                if (valueMap.values().stream().allMatch(this::isBlank)) {
                    continue;
                }
                records.add(new ParsedRecord(rowIndex + 1, valueMap, mapping.converterMap()));
            }
            return records;
        } catch (IllegalArgumentException exception) {
            if (exception.getCause() instanceof MalformedInputException) {
                throw new IllegalArgumentException("Excel 文件不是 UTF-8 编码", exception);
            }
            throw exception;
        }
    }

    private HeaderMapping buildHeaderMapping(List<String> headers) {
        Map<Integer, ColumnMatch> matches = new LinkedHashMap<>();
        Map<String, Function<Double, Double>> converters = new HashMap<>();
        Set<String> assigned = new HashSet<>();
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            ColumnMatch match = matchHeader(header);
            if (match == null || assigned.contains(match.canonicalName())) {
                continue;
            }
            matches.put(index, match);
            converters.put(match.canonicalName(), match.converter());
            assigned.add(match.canonicalName());
        }
        return new HeaderMapping(matches, converters);
    }

    private ColumnMatch matchHeader(String header) {
        if (header == null) {
            return null;
        }
        String normalized = normalizeKey(header);
        for (Map.Entry<String, List<String>> entry : HEADER_SYNONYMS.entrySet()) {
            for (String alias : entry.getValue()) {
                if (normalized.equals(alias) || normalized.contains(alias) || alias.contains(normalized)) {
                    return new ColumnMatch(entry.getKey(), resolveConverter(header, entry.getKey()));
                }
            }
        }
        double bestScore = 0;
        String bestKey = null;
        for (Map.Entry<String, List<String>> entry : HEADER_SYNONYMS.entrySet()) {
            for (String alias : entry.getValue()) {
                double score = SIMILARITY.apply(normalized, alias);
                if (score > bestScore) {
                    bestScore = score;
                    bestKey = entry.getKey();
                }
            }
        }
        if (bestScore >= 0.82 && bestKey != null) {
            return new ColumnMatch(bestKey, resolveConverter(header, bestKey));
        }
        return null;
    }

    private Function<Double, Double> resolveConverter(String header, String canonicalName) {
        if (header == null) {
            return Function.identity();
        }
        String lower = header.toLowerCase(Locale.ROOT);
        if ("sownArea".equals(canonicalName)) {
            if (header.contains("千公顷") || lower.contains("thousand hectare")) {
                return value -> value == null ? null : value * 1000;
            }
            if (header.contains("万亩")) {
                return value -> value == null ? null : value * (10000d / 15d);
            }
            if (header.contains("亩")) {
                return value -> value == null ? null : value / 15d;
            }
            if (header.contains("平方公里") || lower.contains("sq km")) {
                return value -> value == null ? null : value * 100d;
            }
        }
        if ("production".equals(canonicalName)) {
            if (header.contains("万吨")) {
                return value -> value == null ? null : value * 10000d;
            }
            if (header.contains("千吨")) {
                return value -> value == null ? null : value * 1000d;
            }
            if (header.contains("公斤") || header.contains("千克") || lower.contains("kg")) {
                return value -> value == null ? null : value / 1000d;
            }
            if (header.contains("斤")) {
                return value -> value == null ? null : value / 2000d;
            }
        }
        if ("yieldPerHectare".equals(canonicalName)) {
            if (header.contains("公斤/公顷") || header.contains("千克/公顷") || lower.contains("kg/ha")) {
                return value -> value == null ? null : value / 1000d;
            }
            if (header.contains("公斤/亩") || header.contains("千克/亩") || lower.contains("kg/mu")) {
                return value -> value == null ? null : value * 15d / 1000d;
            }
            if (header.contains("斤/亩")) {
                return value -> value == null ? null : value * 7.5d / 1000d;
            }
        }
        if ("averagePrice".equals(canonicalName)) {
            if (header.contains("元/吨") || lower.contains("cny/ton")) {
                return value -> value == null ? null : value / 1000d;
            }
        }
        return Function.identity();
    }

    private Reader createUtf8Reader(Path path) throws IOException {
        return new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8));
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
                yield BigDecimalUtil.toString(cell.getNumericCellValue());
            }
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private DataImportJobView mapToView(DataImportJob job) {
        int total = Optional.ofNullable(job.getTotalRows()).orElse(0);
        int processed = Optional.ofNullable(job.getProcessedRows()).orElse(0);
        int failed = Optional.ofNullable(job.getFailedRows()).orElse(0);
        int skipped = Optional.ofNullable(job.getSkippedRows()).orElse(0);
        double progress = total == 0 ? 0 : Math.min(1.0, (double) (processed + failed + skipped) / total);
        return new DataImportJobView(
                job.getTaskId(),
                job.getDatasetFileId(),
                job.getDatasetName(),
                job.getDatasetType(),
                job.getOriginalFilename(),
                job.getStatus(),
                total,
                processed,
                Optional.ofNullable(job.getInsertedRows()).orElse(0),
                Optional.ofNullable(job.getUpdatedRows()).orElse(0),
                skipped,
                failed,
                Optional.ofNullable(job.getWarningCount()).orElse(0),
                progress,
                job.getMessage(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt()
        );
    }

    private void recordError(List<DataImportJobError> errors, int rowNumber, String code, String message, String rawValue) {
        DataImportJobError error = new DataImportJobError();
        error.setRowNumber(rowNumber);
        error.setErrorCode(code);
        error.setMessage(message);
        error.setRawValue(rawValue);
        errors.add(error);
    }

    private List<String> readPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(payload, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<?> readPreview(String payload, DatasetType datasetType) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            return switch (datasetType) {
                case WEATHER -> objectMapper.readValue(payload, objectMapper.getTypeFactory().constructCollectionType(List.class, WeatherRecordImportPreview.class));
                default -> objectMapper.readValue(payload, objectMapper.getTypeFactory().constructCollectionType(List.class, YieldRecordResponse.class));
            };
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String writePayload(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String writePreview(List<?> preview) {
        try {
            return objectMapper.writeValueAsString(preview);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private List<String> limitList(List<String> source, int limit) {
        if (source.size() <= limit) {
            return source;
        }
        return new ArrayList<>(source.subList(0, limit));
    }

    private Double applyConverter(Function<Double, Double> operator, Double value) {
        if (value == null) {
            return null;
        }
        return operator == null ? value : operator.apply(value);
    }

    private String buildCompletionMessage(DataImportJob job) {
        int failed = Optional.ofNullable(job.getFailedRows()).orElse(0);
        int inserted = Optional.ofNullable(job.getInsertedRows()).orElse(0);
        int updated = Optional.ofNullable(job.getUpdatedRows()).orElse(0);
        StringBuilder builder = new StringBuilder();
        builder.append("任务完成：新增").append(inserted).append("条，更新").append(updated).append("条");
        if (failed > 0) {
            builder.append("，失败").append(failed).append("条");
        }
        if (!job.getErrors().isEmpty()) {
            Map<String, Long> summary = job.getErrors().stream()
                    .collect(Collectors.groupingBy(DataImportJobError::getMessage, Collectors.counting()));
            String top = summary.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(entry -> entry.getValue() + "行：" + entry.getKey())
                    .collect(Collectors.joining("；"));
            if (!top.isEmpty()) {
                builder.append("。失败原因Top：").append(top);
            }
        }
        return builder.toString();
    }

    private String buildDatasetDescription(DataImportJob job) {
        return String.format(Locale.CHINA,
                "导入批次 %s：新增 %d 条，更新 %d 条。%s",
                job.getTaskId(),
                Optional.ofNullable(job.getInsertedRows()).orElse(0),
                Optional.ofNullable(job.getUpdatedRows()).orElse(0),
                Optional.ofNullable(job.getDatasetDescription()).orElse("来自导入中心")
        );
    }

    private Path persistFile(Path root, String datasetName, MultipartFile file) {
        try {
            Files.createDirectories(root);
            String extension = resolveExtension(file.getOriginalFilename());
            String safeBase = datasetName.replaceAll("\\s+", "_");
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
            String filename = timestamp + "_" + safeBase + extension;
            Path target = root.resolve(filename);
            Files.write(target, file.getBytes());
            return target;
        } catch (IOException exception) {
            throw new IllegalArgumentException("保存导入文件失败", exception);
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            return ".xlsx";
        }
        String lower = originalFilename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xls")) {
            return ".xls";
        }
        if (lower.endsWith(".csv")) {
            return ".csv";
        }
        if (lower.endsWith(".xlsx")) {
            return ".xlsx";
        }
        return ".xlsx";
    }

    private String extractBaseName(String originalFilename) {
        String fallback = "数据集" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallback;
        }
        String trimmed = originalFilename.trim();
        int dotIndex = trimmed.lastIndexOf('.');
        String base = dotIndex > 0 ? trimmed.substring(0, dotIndex) : trimmed;
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (base.isEmpty()) {
            return fallback;
        }
        return base;
    }

    private Integer parseYear(String value) {
        Double numeric = parseNumeric(value);
        if (numeric == null) {
            return null;
        }
        return (int) Math.round(numeric);
    }

    private Double parseNumeric(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.replaceAll(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private LocalDate parseWeatherDate(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFKC);
        Matcher matcher = WEATHER_DATE_TOKEN.matcher(normalized);
        String candidate;
        if (matcher.find()) {
            candidate = matcher.group();
        } else {
            candidate = normalized.split("\\s+", 2)[0];
        }
        candidate = candidate.replace("年", "-")
                .replace("月", "-")
                .replace("日", "")
                .replace('/', '-')
                .replace('.', '-');
        candidate = candidate.replaceAll("-+", "-").trim();
        if (candidate.startsWith("-")) {
            candidate = candidate.substring(1);
        }
        if (candidate.endsWith("-")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        if (!candidate.isEmpty()) {
            LocalDate parsed = parseDate(candidate);
            if (parsed != null) {
                return parsed;
            }
        }
        Matcher components = WEATHER_DATE_COMPONENTS.matcher(normalized);
        if (components.find()) {
            try {
                int year = Integer.parseInt(components.group(1));
                int month = Integer.parseInt(components.group(2));
                int day = Integer.parseInt(components.group(3));
                return LocalDate.of(year, month, day);
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private Double parseTemperature(String value) {
        return parseWeatherNumeric(value);
    }

    private Double parseSunshineHours(String value) {
        return parseWeatherNumeric(value);
    }

    private Double parseWeatherNumeric(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.replace('，', ',');
        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Double round(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace("\uFEFF", "");
        String trimmed = cleaned.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRegionLevel(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (trimmed.contains("省") || trimmed.toLowerCase(Locale.ROOT).contains("province")) {
            return "PROVINCE";
        }
        if (trimmed.contains("州") || trimmed.contains("市") || trimmed.toLowerCase(Locale.ROOT).contains("prefecture")) {
            return "PREFECTURE";
        }
        if (trimmed.contains("县") || trimmed.contains("区") || trimmed.toLowerCase(Locale.ROOT).contains("county")) {
            return "COUNTY";
        }
        if (trimmed.contains("乡") || trimmed.contains("镇") || trimmed.contains("村") || trimmed.toLowerCase(Locale.ROOT).contains("township")) {
            return "TOWNSHIP";
        }
        return trimmed.toUpperCase(Locale.ROOT);
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
        return alphanumeric.toUpperCase(Locale.ROOT);
    }

    private String normalizeKey(String input) {
        if (input == null) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{Punct}]", "")
                .toLowerCase(Locale.ROOT);
    }

    private String sanitizeHeader(String header) {
        if (header == null) {
            return null;
        }
        String cleaned = header.replace("\uFEFF", "");
        return cleaned.trim();
    }

    private static Map<String, List<String>> createHeaderSynonyms() {
        Map<String, List<String>> mapping = new HashMap<>();
        mapping.put("cropName", List.of("crop", "cropname", "作物", "作物名称", "品类", "作物(品类)", "种植作物"));
        mapping.put("cropCategory", List.of("category", "cropcategory", "作物类别", "类别", "类型"));
        mapping.put("cropDescription", List.of("cropdescription", "作物描述", "作物说明"));
        mapping.put("regionName", List.of("region", "regionname", "地区", "区域", "州", "市", "地市", "行政区"));
        mapping.put("regionLevel", List.of("regionlevel", "地区级别", "行政级别", "层级", "等级"));
        mapping.put("regionParentName", List.of("parentregion", "parentname", "上级地区", "上级行政区", "所属地区"));
        mapping.put("regionDescription", List.of("regiondescription", "地区描述", "地区说明"));
        mapping.put("year", List.of("year", "年份", "统计年份", "年度"));
        mapping.put("sownArea", List.of("sownarea", "播种面积", "播种总面积", "面积", "耕地面积"));
        mapping.put("production", List.of("production", "产量", "总产量", "产出", "年度产量"));
        mapping.put("yieldPerHectare", List.of("yield", "yieldperhectare", "单产", "产量/面积", "平均单产"));
        mapping.put("averagePrice", List.of("averageprice", "平均价格", "均价", "价格"));
        mapping.put("dataSource", List.of("datasource", "数据来源", "来源", "采集来源"));
        mapping.put("collectedAt", List.of("collectedat", "采集日期", "收集日期", "统计日期"));
        mapping.put("recordDate", List.of("recorddate", "date", "日期", "观测日期", "记录日期"));
        mapping.put("maxTemperature", List.of("maxtemperature", "maxtemp", "最高温", "最高温度", "最高气温"));
        mapping.put("minTemperature", List.of("mintemperature", "mintemp", "最低温", "最低温度", "最低气温"));
        mapping.put("weatherText", List.of("weather", "weathertext", "天气", "天气状况", "天气描述"));
        mapping.put("wind", List.of("wind", "风力风向", "风力", "风向", "风速"));
        mapping.put("sunshineHours", List.of("sunshine", "sunshinehours", "日照时长", "日照时间", "日照小时", "光照时数"));
        return mapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .map(value -> Normalizer.normalize(value, Normalizer.Form.NFKC)
                                .replaceAll("[\\s\\p{Punct}]", "")
                                .toLowerCase(Locale.ROOT))
                        .toList()));
    }

    private static void setNullableDouble(java.sql.PreparedStatement statement, int index, Double value) throws java.sql.SQLException {
        if (value == null) {
            statement.setObject(index, null);
        } else {
            statement.setDouble(index, value);
        }
    }

    private record ParsedRecord(int rowNumber, Map<String, String> values, Map<String, Function<Double, Double>> converters) {
    }

    private record ColumnMatch(String canonicalName, Function<Double, Double> converter) {
    }

    private record HeaderMapping(Map<Integer, ColumnMatch> matches, Map<String, Function<Double, Double>> converterMap) {
    }

    private record YieldRowKey(Long cropId, Long regionId, Integer year) {
    }

    private record WeatherRowKey(Long regionId, LocalDate recordDate) {
    }

    private record ValidRecord(int rowNumber,
                               Crop crop,
                               Region region,
                               int year,
                               Double sownArea,
                               Double production,
                               Double yieldPerHectare,
                               Double averagePrice,
                               String dataSource,
                               LocalDate collectedAt) {
    }

    private record ValidWeatherRecord(int rowNumber,
                                      Region region,
                                      LocalDate recordDate,
                                      Double maxTemperature,
                                      Double minTemperature,
                                      String weatherText,
                                      String wind,
                                      Double sunshineHours,
                                      String dataSource) {
    }

    private record UpsertResult(int inserted, int updated) {
    }

    private record ImportResult(int inserted,
                                int updated,
                                int failed,
                                int skipped,
                                List<String> warnings,
                                List<DataImportJobError> errors,
                                List<?> preview,
                                DatasetFile datasetFile) {
    }

    private static final class BigDecimalUtil {
        private BigDecimalUtil() {
        }

        private static String toString(double value) {
            String text = Double.toString(value);
            if (text.contains("E")) {
                return new java.math.BigDecimal(text).stripTrailingZeros().toPlainString();
            }
            return text;
        }
    }
}
