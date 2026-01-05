package com.gxj.cropyield.dashboard;

import com.gxj.cropyield.dashboard.dto.CropShare;
import com.gxj.cropyield.dashboard.dto.DashboardSummaryResponse;
import com.gxj.cropyield.dashboard.dto.ForecastPoint;
import com.gxj.cropyield.dashboard.dto.RecentYieldRecord;
import com.gxj.cropyield.dashboard.dto.RegionProductionSummary;
import com.gxj.cropyield.dashboard.dto.TrendPoint;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.forecast.entity.ForecastSnapshot;
import com.gxj.cropyield.modules.forecast.repository.ForecastSnapshotRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
/**
 * 驾驶舱统计模块的业务接口，定义驾驶舱统计相关的核心业务操作。
 * <p>核心方法：getSummary、buildProductionTrend、buildCropStructure、buildRegionComparisons、buildRecentRecords、round、buildForecast、safe、exportForecastDataAsPdf。</p>
 */

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String DEFAULT_FONT = "STSong-Light";

    private final YieldRecordRepository yieldRecordRepository;
    private final ForecastSnapshotRepository forecastSnapshotRepository;

    public DashboardService(YieldRecordRepository yieldRecordRepository,
                            ForecastSnapshotRepository forecastSnapshotRepository) {
        this.yieldRecordRepository = yieldRecordRepository;
        this.forecastSnapshotRepository = forecastSnapshotRepository;
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
        try {
            List<ForecastSnapshot> snapshots = forecastSnapshotRepository
                    .findByOrderByCreatedAtDesc();
            if (snapshots.isEmpty()) {
                return List.of();
            }
            return snapshots.stream()
                    .map(snapshot -> {
                        var run = snapshot.getRun();
                        LocalDate collectedAt = run.getUpdatedAt() != null
                                ? run.getUpdatedAt().toLocalDate()
                                : LocalDate.now();
                        int year = snapshot.getYear() != null
                                ? snapshot.getYear()
                                : collectedAt.getYear();
                        double sownArea = safe(snapshot.getSownArea());
                        double production = safe(snapshot.getPredictedProduction());
                        double yield = safe(snapshot.getPredictedYield());
                        return new RecentYieldRecord(
                                run.getId(),
                                run.getCrop().getName(),
                                run.getRegion().getName(),
                                year,
                                round(sownArea),
                                round(production),
                                round(yield),
                                collectedAt
                        );
                    })
                    .toList();
        } catch (DataAccessException ex) {
            log.warn("Failed to load forecast snapshots for dashboard recent records, returning empty list", ex);
            return List.of();
        }
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

    public byte[] exportForecastDataAsPdf(String cropFilter, String regionFilter, Integer yearFilter) {
        List<RecentYieldRecord> records = buildRecentRecords();
        
        // 应用过滤器
        if (StringUtils.hasText(cropFilter) && !"ALL".equals(cropFilter)) {
            records = records.stream()
                    .filter(r -> cropFilter.equals(r.cropName()))
                    .toList();
        }
        if (StringUtils.hasText(regionFilter) && !"ALL".equals(regionFilter)) {
            records = records.stream()
                    .filter(r -> regionFilter.equals(r.regionName()))
                    .toList();
        }
        if (yearFilter != null) {
            records = records.stream()
                    .filter(r -> yearFilter.equals(r.year()))
                    .toList();
        }
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont baseFont = BaseFont.createFont(DEFAULT_FONT, "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font headerFont = new Font(baseFont, 11, Font.BOLD);
            Font bodyFont = new Font(baseFont, 10);

            Paragraph title = new Paragraph(sanitizeForPdf("仪表盘预测数据导出"), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(sanitizeForPdf("导出时间：" + DATE_FORMATTER.format(LocalDate.now())), bodyFont));
            document.add(new Paragraph(sanitizeForPdf(buildFilterSummary(cropFilter, regionFilter, yearFilter)), bodyFont));
            document.add(new Paragraph(sanitizeForPdf("记录总数：" + records.size()), bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{8, 18, 15, 14, 14, 14, 14});

            addHeaderCell(table, sanitizeForPdf("年份"), headerFont);
            addHeaderCell(table, sanitizeForPdf("地区"), headerFont);
            addHeaderCell(table, sanitizeForPdf("作物"), headerFont);
            addHeaderCell(table, sanitizeForPdf("播种面积(公顷)"), headerFont);
            addHeaderCell(table, sanitizeForPdf("总产量(吨)"), headerFont);
            addHeaderCell(table, sanitizeForPdf("单产(吨/公顷)"), headerFont);
            addHeaderCell(table, sanitizeForPdf("数据日期"), headerFont);

            for (RecentYieldRecord record : records) {
                addBodyCell(table, String.valueOf(record.year()), bodyFont);
                addBodyCell(table, sanitizeForPdf(record.regionName()), bodyFont);
                addBodyCell(table, sanitizeForPdf(record.cropName()), bodyFont);
                addBodyCell(table, formatNumber(record.sownArea()), bodyFont);
                addBodyCell(table, formatNumber(record.production()), bodyFont);
                addBodyCell(table, formatNumber(record.yieldPerHectare()), bodyFont);
                addBodyCell(table, formatDate(record.collectedAt()), bodyFont);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("导出 PDF 失败", exception);
        }
    }

    private void addHeaderCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private String buildFilterSummary(String cropFilter, String regionFilter, Integer yearFilter) {
        List<String> parts = new ArrayList<>();
        
        if (StringUtils.hasText(cropFilter) && !"ALL".equals(cropFilter)) {
            parts.add("作物：" + cropFilter);
        } else {
            parts.add("作物：全部");
        }
        
        if (StringUtils.hasText(regionFilter) && !"ALL".equals(regionFilter)) {
            parts.add("地区：" + regionFilter);
        } else {
            parts.add("地区：全部");
        }
        
        if (yearFilter != null) {
            parts.add("年份：" + yearFilter);
        } else {
            parts.add("年份：全部");
        }
        
        return String.join("，", parts);
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.CHINA, "%.2f", value);
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date);
    }

    private String sanitizeForPdf(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text
            .replace("²", "2")
            .replace("³", "3")
            .replace("±", "+/-")
            .replace("×", "x")
            .replace("÷", "/")
            .replace("≈", "~")
            .replace("≤", "<=")
            .replace("≥", ">=")
            .replace("≠", "!=")
            .replace("→", "->")
            .replace("←", "<-")
            .replace("↑", "^")
            .replace("↓", "v")
            .replace("•", "* ")
            .replace("◦", "o ")
            .replace("▪", "* ")
            .replace("■", "* ")
            .replace("□", "o ")
            .replace("▲", "^ ")
            .replace("△", "^ ")
            .replace("▼", "v ")
            .replace("►", "> ")
            .replace("◄", "< ")
            .replace("◆", "* ")
            .replace("◇", "o ")
            .replace("★", "* ")
            .replace("☆", "* ")
            .replace("●", "* ")
            .replace("○", "o ")
            .replace("€", "EUR ")
            .replace("£", "GBP ")
            .replace("¥", "CNY ")
            .replace("$", "USD ")
            .replace("©", "(c) ")
            .replace("®", "(R) ")
            .replace("™", "(TM) ")
            .replace("°", " degrees ")
            .replace("℃", "C ")
            .replace("℉", "F ")
            .replace("…", "...")
            .replace("–", "-")
            .replace("—", "--")
            .replace("'", "'")
            .replace("'", "'")
            .replace("\u201C", "\"")
            .replace("\u201D", "\"")
            .replace("«", "<<")
            .replace("»", ">>")
            .replace("¡", "!")
            .replace("¿", "?");
    }
}
