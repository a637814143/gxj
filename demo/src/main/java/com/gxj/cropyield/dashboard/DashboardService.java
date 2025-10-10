package com.gxj.cropyield.dashboard;

import com.gxj.cropyield.dashboard.dto.CropShare;
import com.gxj.cropyield.dashboard.dto.DashboardSummaryResponse;
import com.gxj.cropyield.dashboard.dto.ForecastPoint;
import com.gxj.cropyield.dashboard.dto.RecentYieldRecord;
import com.gxj.cropyield.dashboard.dto.RegionProductionSummary;
import com.gxj.cropyield.dashboard.dto.TrendPoint;
import com.gxj.cropyield.yielddata.YieldRecord;
import com.gxj.cropyield.yielddata.YieldRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final YieldRecordRepository yieldRecordRepository;

    public DashboardService(YieldRecordRepository yieldRecordRepository) {
        this.yieldRecordRepository = yieldRecordRepository;
    }

    public DashboardSummaryResponse getSummary() {
        List<YieldRecord> records = yieldRecordRepository.findAll();
        if (records.isEmpty()) {
            return new DashboardSummaryResponse(
                    0,
                    0,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        double totalProduction = records.stream()
                .mapToDouble(record -> safe(record.getProduction()))
                .sum();
        double totalArea = records.stream()
                .mapToDouble(record -> safe(record.getSownArea()))
                .sum();
        double averageYield = totalArea > 0 ? totalProduction / totalArea : 0;
        long recordCount = records.size();

        List<TrendPoint> productionTrend = buildProductionTrend(records);
        List<CropShare> cropStructure = buildCropStructure(records, totalProduction);
        List<RegionProductionSummary> regionComparisons = buildRegionComparisons(records);
        List<RecentYieldRecord> recentRecords = buildRecentRecords();
        List<ForecastPoint> forecastPoints = buildForecast(records);

        return new DashboardSummaryResponse(
                round(totalProduction),
                round(totalArea),
                round(averageYield),
                recordCount,
                productionTrend,
                cropStructure,
                regionComparisons,
                recentRecords,
                forecastPoints
        );
    }

    private List<TrendPoint> buildProductionTrend(List<YieldRecord> records) {
        Map<Integer, DoubleSummaryStatistics> statsByYear = records.stream()
                .collect(Collectors.groupingBy(
                        YieldRecord::getYear,
                        TreeMap::new,
                        Collectors.summarizingDouble(record -> safe(record.getProduction()))
                ));

        return statsByYear.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .skip(Math.max(0, statsByYear.size() - 6))
                .map(entry -> new TrendPoint(entry.getKey() + "年", round(entry.getValue().getSum())))
                .toList();
    }

    private List<CropShare> buildCropStructure(List<YieldRecord> records, double totalProduction) {
        Map<String, double[]> map = new TreeMap<>();
        records.forEach(record -> {
            String cropName = record.getCrop().getName();
            double[] values = map.computeIfAbsent(cropName, ignored -> new double[2]);
            values[0] += safe(record.getProduction());
            values[1] += safe(record.getSownArea());
        });

        return map.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .map(entry -> {
                    double production = entry.getValue()[0];
                    double area = entry.getValue()[1];
                    double share = totalProduction > 0 ? production / totalProduction : 0;
                    return new CropShare(entry.getKey(), round(production), round(area), round(share));
                })
                .toList();
    }

    private List<RegionProductionSummary> buildRegionComparisons(List<YieldRecord> records) {
        Map<String, double[]> map = new TreeMap<>();
        records.forEach(record -> {
            String key = record.getRegion().getName() + "::" + record.getRegion().getLevel();
            double[] values = map.computeIfAbsent(key, ignored -> new double[2]);
            values[0] += safe(record.getProduction());
            values[1] += safe(record.getSownArea());
        });

        return map.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .map(entry -> {
                    String[] parts = entry.getKey().split("::");
                    double production = entry.getValue()[0];
                    double area = entry.getValue()[1];
                    double yield = area > 0 ? production / area : 0;
                    return new RegionProductionSummary(parts[0], parts[1], round(production), round(area), round(yield));
                })
                .toList();
    }

    private List<RecentYieldRecord> buildRecentRecords() {
        return yieldRecordRepository.findTop5ByOrderByYearDesc().stream()
                .sorted(Comparator.comparing(YieldRecord::getYear).reversed())
                .map(record -> {
                    double revenue = safe(record.getProduction()) * safe(record.getAveragePrice()) * 0.1;
                    return new RecentYieldRecord(
                            record.getCrop().getName(),
                            record.getRegion().getName(),
                            record.getYear(),
                            round(safe(record.getSownArea())),
                            round(safe(record.getProduction())),
                            round(safe(record.getYieldPerHectare())),
                            record.getAveragePrice(),
                            round(revenue),
                            record.getCollectedAt() != null ? record.getCollectedAt() : LocalDate.of(record.getYear(), 12, 31)
                    );
                })
                .toList();
    }

    private List<ForecastPoint> buildForecast(List<YieldRecord> records) {
        Map<Integer, Double> productionByYear = records.stream()
                .collect(Collectors.groupingBy(
                        YieldRecord::getYear,
                        TreeMap::new,
                        Collectors.summingDouble(record -> safe(record.getProduction()))
                ));

        if (productionByYear.size() < 2) {
            return List.of();
        }

        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(productionByYear.entrySet());
        sorted.sort(Map.Entry.comparingByKey());

        int firstYear = sorted.get(Math.max(0, sorted.size() - Math.min(5, sorted.size()))).getKey();
        double firstValue = sorted.get(Math.max(0, sorted.size() - Math.min(5, sorted.size()))).getValue();
        int lastYear = sorted.get(sorted.size() - 1).getKey();
        double lastValue = sorted.get(sorted.size() - 1).getValue();
        int period = lastYear - firstYear;

        double growthRate = 0.0;
        if (period > 0 && firstValue > 0) {
            growthRate = Math.pow(lastValue / firstValue, 1.0 / period) - 1.0;
        }

        List<ForecastPoint> points = new ArrayList<>();
        double baseline = lastValue;
        for (int i = 1; i <= 3; i++) {
            baseline = baseline * (1 + growthRate);
            double lower = baseline * 0.9;
            double upper = baseline * 1.1;
            int forecastYear = lastYear + i;
            points.add(new ForecastPoint(
                    forecastYear + "年",
                    round(baseline),
                    round(lower),
                    round(upper),
                    "ARIMA 模拟"
            ));
        }
        return points;
    }

    private double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
