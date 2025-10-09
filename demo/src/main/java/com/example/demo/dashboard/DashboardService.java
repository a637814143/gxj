package com.example.demo.dashboard;

import com.example.demo.category.CategoryRepository;
import com.example.demo.dashboard.dto.DashboardSummaryResponse;
import com.example.demo.dashboard.dto.RecentPredictionSummary;
import com.example.demo.dashboard.dto.TrendPoint;
import com.example.demo.dashboard.dto.UpcomingRenewalSummary;
import com.example.demo.prediction.UsagePrediction;
import com.example.demo.prediction.UsagePredictionRepository;
import com.example.demo.software.SoftwareAsset;
import com.example.demo.software.SoftwareAssetRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final SoftwareAssetRepository softwareAssetRepository;
    private final UsagePredictionRepository usagePredictionRepository;
    private final CategoryRepository categoryRepository;

    public DashboardService(SoftwareAssetRepository softwareAssetRepository,
                            UsagePredictionRepository usagePredictionRepository,
                            CategoryRepository categoryRepository) {
        this.softwareAssetRepository = softwareAssetRepository;
        this.usagePredictionRepository = usagePredictionRepository;
        this.categoryRepository = categoryRepository;
    }

    public DashboardSummaryResponse getSummary() {
        long softwareCount = softwareAssetRepository.count();
        long predictedSoftwareCount = usagePredictionRepository.countDistinctSoftware();
        long predictionCount = usagePredictionRepository.count();
        long categoryCount = categoryRepository.count();

        List<TrendPoint> trendPoints = buildTrendPoints();
        List<RecentPredictionSummary> latestPredictions = mapLatestPredictions();
        List<UpcomingRenewalSummary> upcomingRenewals = mapUpcomingRenewals();

        return new DashboardSummaryResponse(
                softwareCount,
                predictedSoftwareCount,
                predictionCount,
                categoryCount,
                trendPoints,
                latestPredictions,
                upcomingRenewals
        );
    }

    private List<TrendPoint> buildTrendPoints() {
        LocalDate latestPredictionDate = Optional.ofNullable(usagePredictionRepository.findLatestPredictionDate())
                .orElse(LocalDate.now());
        YearMonth latestMonth = YearMonth.from(latestPredictionDate);
        YearMonth startMonth = latestMonth.minusMonths(5);

        List<UsagePrediction> predictions = usagePredictionRepository.findByPredictionDateBetween(
                startMonth.atDay(1), latestMonth.atEndOfMonth());

        Map<YearMonth, Long> grouped = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = startMonth.plusMonths(i);
            grouped.put(month, 0L);
        }

        predictions.forEach(prediction -> {
            YearMonth month = YearMonth.from(prediction.getPredictionDate());
            grouped.computeIfPresent(month, (ignored, value) -> value + 1);
        });

        List<TrendPoint> result = new ArrayList<>();
        grouped.forEach((month, count) -> result.add(new TrendPoint(month.toString(), count)));
        return result;
    }

    private List<RecentPredictionSummary> mapLatestPredictions() {
        return usagePredictionRepository.findTop5ByOrderByPredictionDateDesc()
                .stream()
                .map(prediction -> new RecentPredictionSummary(
                        prediction.getSoftware().getName(),
                        prediction.getSoftware().getCategory() != null ? prediction.getSoftware().getCategory().getName() : "",
                        prediction.getPredictionDate(),
                        prediction.getPredictedAnnualCost(),
                        prediction.getPredictedActiveUsers(),
                        prediction.getConfidence() != null ? prediction.getConfidence().doubleValue() : null
                ))
                .collect(Collectors.toList());
    }

    private List<UpcomingRenewalSummary> mapUpcomingRenewals() {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(120);

        List<SoftwareAsset> assets = softwareAssetRepository
                .findByMaintenanceExpiryDateBetweenOrderByMaintenanceExpiryDate(today, limit);

        return assets.stream()
                .map(asset -> new UpcomingRenewalSummary(
                        asset.getName(),
                        asset.getCategory() != null ? asset.getCategory().getName() : "",
                        asset.getMaintenanceExpiryDate(),
                        asset.getMaintenanceExpiryDate() != null ? ChronoUnit.DAYS.between(today, asset.getMaintenanceExpiryDate()) : 0,
                        asset.getLicenseType(),
                        asset.getVendor(),
                        usagePredictionRepository.existsBySoftwareId(asset.getId())
                ))
                .limit(6)
                .collect(Collectors.toList());
    }
}
