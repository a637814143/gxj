package com.example.demo.software;

import com.example.demo.category.Category;
import com.example.demo.category.CategoryRepository;
import com.example.demo.prediction.UsagePredictionRepository;
import com.example.demo.software.dto.CategorySummaryResponse;
import com.example.demo.software.dto.SoftwareAssetResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SoftwareAssetService {

    private final SoftwareAssetRepository softwareAssetRepository;
    private final UsagePredictionRepository usagePredictionRepository;
    private final CategoryRepository categoryRepository;

    public SoftwareAssetService(SoftwareAssetRepository softwareAssetRepository,
                                UsagePredictionRepository usagePredictionRepository,
                                CategoryRepository categoryRepository) {
        this.softwareAssetRepository = softwareAssetRepository;
        this.usagePredictionRepository = usagePredictionRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<SoftwareAssetResponse> getSoftwareAssets(String search,
                                                          Long categoryId,
                                                          SoftwareStatus status,
                                                          Boolean expiringSoon) {
        List<SoftwareAsset> assets = softwareAssetRepository.findAll();

        LocalDate today = LocalDate.now();
        List<SoftwareAsset> filtered = assets.stream()
                .filter(asset -> matchesSearch(asset, search))
                .filter(asset -> matchesCategory(asset, categoryId))
                .filter(asset -> matchesStatus(asset, status))
                .filter(asset -> matchesExpiry(asset, expiringSoon, today))
                .sorted(assetComparator())
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> assetIds = filtered.stream()
                .map(SoftwareAsset::getId)
                .collect(Collectors.toList());

        Set<Long> predictedSoftwareIds = assetIds.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(usagePredictionRepository.findSoftwareIdsWithPredictions(assetIds));

        return filtered.stream()
                .map(asset -> mapToResponse(asset, predictedSoftwareIds.contains(asset.getId()), today))
                .collect(Collectors.toList());
    }

    public List<CategorySummaryResponse> getCategorySummaries() {
        List<Category> categories = categoryRepository.findAll();
        List<SoftwareAsset> assets = softwareAssetRepository.findAll();

        Map<Long, List<SoftwareAsset>> groupedByCategory = new HashMap<>();
        List<SoftwareAsset> uncategorized = new ArrayList<>();

        for (SoftwareAsset asset : assets) {
            if (asset.getCategory() != null) {
                groupedByCategory
                        .computeIfAbsent(asset.getCategory().getId(), ignored -> new ArrayList<>())
                        .add(asset);
            } else {
                uncategorized.add(asset);
            }
        }

        Set<Long> predictedSoftwareIds = assets.isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(usagePredictionRepository.findSoftwareIdsWithPredictions(
                assets.stream().map(SoftwareAsset::getId).collect(Collectors.toList())));

        List<CategorySummaryResponse> responses = categories.stream()
                .sorted(Comparator.comparing(category -> Optional.ofNullable(category.getName()).orElse(""), String.CASE_INSENSITIVE_ORDER))
                .map(category -> buildCategorySummary(
                        category,
                        groupedByCategory.getOrDefault(category.getId(), Collections.emptyList()),
                        predictedSoftwareIds))
                .collect(Collectors.toList());

        if (!uncategorized.isEmpty()) {
            responses.add(buildCategorySummary(
                    new CategoryPlaceholder(),
                    uncategorized,
                    predictedSoftwareIds));
        }

        return responses;
    }

    private SoftwareAssetResponse mapToResponse(SoftwareAsset asset,
                                                boolean hasPrediction,
                                                LocalDate today) {
        LocalDate expiry = asset.getMaintenanceExpiryDate();
        Long daysRemaining = expiry != null ? ChronoUnit.DAYS.between(today, expiry) : null;
        boolean expired = daysRemaining != null && daysRemaining < 0;
        boolean expiringSoon = daysRemaining != null && !expired && daysRemaining <= 30;

        return new SoftwareAssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getVersion(),
                asset.getVendor(),
                asset.getCategory() != null ? asset.getCategory().getName() : null,
                Optional.ofNullable(asset.getStatus()).map(Enum::name).orElse(null),
                asset.getLicenseType(),
                asset.getSeats(),
                asset.getAnnualCost(),
                expiry,
                daysRemaining,
                expired,
                expiringSoon,
                hasPrediction
        );
    }

    private CategorySummaryResponse buildCategorySummary(Category category,
                                                          List<SoftwareAsset> assets,
                                                          Set<Long> predictedIds) {
        long total = assets.size();
        long active = assets.stream()
                .filter(asset -> SoftwareStatus.ACTIVE.equals(asset.getStatus()))
                .count();
        long predicted = assets.stream()
                .filter(asset -> predictedIds.contains(asset.getId()))
                .count();
        BigDecimal annualCost = assets.stream()
                .map(asset -> asset.getAnnualCost() != null ? asset.getAnnualCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CategorySummaryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                total,
                active,
                predicted,
                annualCost
        );
    }

    private boolean matchesSearch(SoftwareAsset asset, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        String keyword = search.toLowerCase();
        return StreamBuilder.from(asset.getName())
                .add(asset.getVendor())
                .add(asset.getLicenseType())
                .add(asset.getVersion())
                .anyMatch(value -> value.toLowerCase().contains(keyword));
    }

    private boolean matchesCategory(SoftwareAsset asset, Long categoryId) {
        if (categoryId == null) {
            return true;
        }
        return asset.getCategory() != null && categoryId.equals(asset.getCategory().getId());
    }

    private boolean matchesStatus(SoftwareAsset asset, SoftwareStatus status) {
        if (status == null) {
            return true;
        }
        return status.equals(asset.getStatus());
    }

    private boolean matchesExpiry(SoftwareAsset asset, Boolean expiringSoon, LocalDate today) {
        if (!Boolean.TRUE.equals(expiringSoon)) {
            return true;
        }
        LocalDate expiry = asset.getMaintenanceExpiryDate();
        if (expiry == null) {
            return false;
        }
        return !expiry.isBefore(today) && !expiry.isAfter(today.plusDays(60));
    }

    private Comparator<SoftwareAsset> assetComparator() {
        return Comparator
                .comparing((SoftwareAsset asset) -> Optional.ofNullable(asset.getMaintenanceExpiryDate()).orElse(LocalDate.MAX))
                .thenComparing(asset -> Optional.ofNullable(asset.getName()).orElse(""), String.CASE_INSENSITIVE_ORDER);
    }

    private static class CategoryPlaceholder extends Category {
        @Override
        public Long getId() {
            return null;
        }

        @Override
        public String getName() {
            return "未分类";
        }

        @Override
        public String getDescription() {
            return "暂无分类";
        }
    }

    private static class StreamBuilder {
        private final List<String> values = new ArrayList<>();

        private StreamBuilder(String initial) {
            add(initial);
        }

        public static StreamBuilder from(String value) {
            return new StreamBuilder(value);
        }

        public StreamBuilder add(String value) {
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
            return this;
        }

        public boolean anyMatch(java.util.function.Predicate<String> predicate) {
            return values.stream().anyMatch(predicate);
        }
    }
}
