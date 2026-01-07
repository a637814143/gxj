package com.gxj.cropyield.modules.dataset.service.impl;

import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.dataset.dto.WeatherCropOption;
import com.gxj.cropyield.modules.dataset.dto.WeatherDatasetResponse;
import com.gxj.cropyield.modules.dataset.dto.WeatherDatasetSummary;
import com.gxj.cropyield.modules.dataset.dto.WeatherRecordView;
import com.gxj.cropyield.modules.dataset.dto.WeatherRegionOption;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import com.gxj.cropyield.modules.dataset.entity.WeatherRecord;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.WeatherRecordRepository;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.dataset.service.WeatherRecordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 气象数据管理服务实现类 - 气象记录CRUD
 */
@Service
public class WeatherRecordServiceImpl implements WeatherRecordService {

    private static final int MAX_PAGE_SIZE = 1000;

    private final WeatherRecordRepository weatherRecordRepository;
    private final YieldRecordRepository yieldRecordRepository;

    public WeatherRecordServiceImpl(WeatherRecordRepository weatherRecordRepository,
                                    YieldRecordRepository yieldRecordRepository) {
        this.weatherRecordRepository = weatherRecordRepository;
        this.yieldRecordRepository = yieldRecordRepository;
    }

    @Override
    public WeatherDatasetResponse queryDataset(Long regionId, LocalDate startDate, LocalDate endDate, Integer limit) {
        Specification<WeatherRecord> specification = (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            if (regionId != null) {
                predicate = builder.and(predicate, builder.equal(root.get("region").get("id"), regionId));
            }
            if (startDate != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("recordDate"), startDate));
            }
            if (endDate != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("recordDate"), endDate));
            }
            return predicate;
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "recordDate");
        List<WeatherRecord> records;
        int safeLimit = limit == null ? 0 : Math.max(0, Math.min(limit, MAX_PAGE_SIZE));
        if (safeLimit > 0) {
            Pageable pageable = PageRequest.of(0, safeLimit, sort);
            Page<WeatherRecord> page = weatherRecordRepository.findAll(specification, pageable);
            records = page.getContent();
        } else {
            records = weatherRecordRepository.findAll(specification, sort);
        }

        List<WeatherRecordView> views = records.stream()
                .map(this::mapToView)
                .toList();

        WeatherDatasetSummary summary = buildSummary(records);
        return new WeatherDatasetResponse(summary, views);
    }

    @Override
    public List<WeatherRegionOption> listRegionsWithRecords() {
        List<WeatherRecord> records = weatherRecordRepository.findAll();

        Map<Long, Long> counts = new HashMap<>();
        Map<Long, String> names = new HashMap<>();

        for (WeatherRecord record : records) {
            if (record.getRegion() == null) {
                continue;
            }
            String dataSource = resolveDataSource(record);
            if (!StringUtils.hasText(dataSource)) {
                continue;
            }
            String normalized = dataSource.toLowerCase(Locale.ROOT);
            if (normalized.contains("示例") || normalized.contains("sample")) {
                continue;
            }

            Long regionId = record.getRegion().getId();
            counts.merge(regionId, 1L, Long::sum);
            names.putIfAbsent(regionId, record.getRegion().getName());
        }

        List<WeatherRegionOption> options = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            Long regionId = entry.getKey();
            String regionName = names.get(regionId);
            if (!StringUtils.hasText(regionName)) {
                continue;
            }
            options.add(new WeatherRegionOption(regionId, regionName, entry.getValue()));
        }

        options.sort(Comparator.comparing(WeatherRegionOption::regionName, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    @Override
    public List<WeatherCropOption> listCropsWithRecords(Long regionId) {
        if (regionId == null) {
            return Collections.emptyList();
        }

        List<YieldRecord> records = yieldRecordRepository.findByRegionId(regionId);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> counts = new HashMap<>();
        Map<Long, String> names = new HashMap<>();

        for (YieldRecord record : records) {
            Crop crop = record.getCrop();
            if (crop == null) {
                continue;
            }

            String dataSource = resolveDataSource(record);
            if (!StringUtils.hasText(dataSource)) {
                continue;
            }
            String normalized = dataSource.toLowerCase(Locale.ROOT);
            if (normalized.contains("示例") || normalized.contains("sample")) {
                continue;
            }

            Long cropId = crop.getId();
            if (cropId == null) {
                continue;
            }

            counts.merge(cropId, 1L, Long::sum);
            names.putIfAbsent(cropId, crop.getName());
        }

        List<WeatherCropOption> options = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            Long cropId = entry.getKey();
            String cropName = names.get(cropId);
            if (!StringUtils.hasText(cropName)) {
                continue;
            }
            options.add(new WeatherCropOption(cropId, cropName, entry.getValue()));
        }

        options.sort(Comparator.comparing(WeatherCropOption::cropName, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    private WeatherDatasetSummary buildSummary(List<WeatherRecord> records) {
        long total = records.size();

        Double avgMax = average(records, WeatherRecord::getMaxTemperature);
        Double avgMin = average(records, WeatherRecord::getMinTemperature);
        Double totalSunshine = sum(records, WeatherRecord::getSunshineHours);
        Double avgSunshine = total > 0 && totalSunshine != null ? round(totalSunshine / total) : null;

        Optional<WeatherRecord> hottest = records.stream()
                .filter(record -> record.getMaxTemperature() != null)
                .max(Comparator.comparing(WeatherRecord::getMaxTemperature));
        Optional<WeatherRecord> coldest = records.stream()
                .filter(record -> record.getMinTemperature() != null)
                .min(Comparator.comparing(WeatherRecord::getMinTemperature));

        Map<String, Long> weatherCounts = records.stream()
                .map(WeatherRecord::getWeatherText)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Optional<Map.Entry<String, Long>> dominantWeather = weatherCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        Double dominantRatio = dominantWeather
                .filter(entry -> total > 0)
                .map(entry -> round((double) entry.getValue() / total))
                .orElse(null);

        LocalDate firstRecordDate = records.stream()
                .map(WeatherRecord::getRecordDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastRecordDate = records.stream()
                .map(WeatherRecord::getRecordDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        return new WeatherDatasetSummary(
                total,
                avgMax,
                avgMin,
                hottest.map(WeatherRecord::getMaxTemperature).orElse(null),
                hottest.map(WeatherRecord::getRecordDate).orElse(null),
                coldest.map(WeatherRecord::getMinTemperature).orElse(null),
                coldest.map(WeatherRecord::getRecordDate).orElse(null),
                totalSunshine,
                avgSunshine,
                dominantWeather.map(Map.Entry::getKey).orElse(null),
                dominantRatio,
                firstRecordDate,
                lastRecordDate
        );
    }

    private WeatherRecordView mapToView(WeatherRecord record) {
        String resolvedDataSource = resolveDataSource(record);

        return new WeatherRecordView(
                record.getId(),
                Optional.ofNullable(record.getRegion()).map(r -> r.getId()).orElse(null),
                Optional.ofNullable(record.getRegion()).map(r -> r.getName()).orElse(null),
                record.getRecordDate(),
                record.getMaxTemperature(),
                record.getMinTemperature(),
                record.getWeatherText(),
                record.getWind(),
                record.getSunshineHours(),
                resolvedDataSource,
                Optional.ofNullable(record.getDatasetFile()).map(DatasetFile::getName).orElse(null)
        );
    }

    private String resolveDataSource(WeatherRecord record) {
        return Optional.ofNullable(record.getDataSource())
                .map(String::trim)
                .filter(source -> !source.isEmpty())
                .orElseGet(() -> Optional.ofNullable(record.getDatasetFile())
                        .map(file -> Optional.ofNullable(file.getDescription()).orElse(file.getName()))
                        .map(String::trim)
                        .filter(source -> !source.isEmpty())
                        .orElse(null));
    }

    private String resolveDataSource(YieldRecord record) {
        return Optional.ofNullable(record.getDataSource())
                .map(String::trim)
                .filter(source -> !source.isEmpty())
                .orElseGet(() -> Optional.ofNullable(record.getDatasetFile())
                        .map(file -> Optional.ofNullable(file.getDescription()).orElse(file.getName()))
                        .map(String::trim)
                        .filter(source -> !source.isEmpty())
                        .orElse(null));
    }

    private Double average(List<WeatherRecord> records, Function<WeatherRecord, Double> extractor) {
        List<Double> values = records.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        return round(sum / values.size());
    }

    private Double sum(List<WeatherRecord> records, Function<WeatherRecord, Double> extractor) {
        double total = 0d;
        int count = 0;
        for (WeatherRecord record : records) {
            Double value = extractor.apply(record);
            if (value != null) {
                total += value;
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return round(total);
    }

    private Double round(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
