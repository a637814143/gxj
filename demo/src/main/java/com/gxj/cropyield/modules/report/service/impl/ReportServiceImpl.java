package com.gxj.cropyield.modules.report.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
import com.gxj.cropyield.modules.forecast.entity.ForecastResult;
import com.gxj.cropyield.modules.forecast.repository.ForecastResultRepository;
import com.gxj.cropyield.modules.report.dto.ReportDetailResponse;
import com.gxj.cropyield.modules.report.dto.ReportGenerationRequest;
import com.gxj.cropyield.modules.report.dto.ReportMetrics;
import com.gxj.cropyield.modules.report.dto.ReportOverviewResponse;
import com.gxj.cropyield.modules.report.dto.ReportRequest;
import com.gxj.cropyield.modules.report.dto.ReportSectionResponse;
import com.gxj.cropyield.modules.report.dto.ReportSummaryResponse;
import com.gxj.cropyield.modules.report.entity.Report;
import com.gxj.cropyield.modules.report.entity.ReportSection;
import com.gxj.cropyield.modules.report.repository.ReportRepository;
import com.gxj.cropyield.modules.report.service.ReportExportResult;
import com.gxj.cropyield.modules.report.service.ReportService;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * 报表分析模块的业务实现类，负责落实报表分析领域的业务处理逻辑。
 * <p>核心方法：getOverview、create、generate、getDetail、toDetail、toSummary、buildSection、toSectionResponse。</p>
 */

@Service
public class ReportServiceImpl implements ReportService {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private static final String DEFAULT_FONT = "STSongStd-Light";

    private static final java.time.format.DateTimeFormatter DATE_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportRepository reportRepository;
    private final ForecastResultRepository forecastResultRepository;
    private final RegionRepository regionRepository;
    private final CropRepository cropRepository;
    private final YieldRecordRepository yieldRecordRepository;
    private final ObjectMapper objectMapper;

    public ReportServiceImpl(ReportRepository reportRepository,
                             ForecastResultRepository forecastResultRepository,
                             RegionRepository regionRepository,
                             CropRepository cropRepository,
                             YieldRecordRepository yieldRecordRepository,
                             ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.forecastResultRepository = forecastResultRepository;
        this.regionRepository = regionRepository;
        this.cropRepository = cropRepository;
        this.yieldRecordRepository = yieldRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReportOverviewResponse getOverview() {
        List<Report> reports = reportRepository.findAll(Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt"));
        List<ReportSummaryResponse> summaries = reports.stream()
            .map(this::toSummary)
            .collect(Collectors.toList());

        YearMonth currentMonth = YearMonth.now();
        long publishedThisMonth = reports.stream()
            .filter(report -> report.getPublishedAt() != null)
            .filter(report -> YearMonth.from(report.getPublishedAt()).equals(currentMonth))
            .filter(report -> "PUBLISHED".equalsIgnoreCase(report.getStatus()))
            .count();

        long pendingApproval = reports.stream()
            .filter(report -> "PENDING".equalsIgnoreCase(report.getStatus()))
            .count();

        boolean autoGenerationEnabled = reports.stream().anyMatch(Report::isAutoGenerated);

        return new ReportOverviewResponse(
            summaries,
            new ReportMetrics(reports.size(), publishedThisMonth, pendingApproval, autoGenerationEnabled)
        );
    }

    @Override
    @Transactional
    public Report create(ReportRequest request) {
        ForecastResult result = forecastResultRepository.findById(request.forecastResultId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "预测结果不存在"));

        Report report = new Report();
        report.setTitle(request.title());
        report.setDescription(request.description());
        report.setAuthor(StringUtils.hasText(request.author()) ? request.author().trim() : "系统自动");
        report.setCoveragePeriod(StringUtils.hasText(request.coveragePeriod()) ? request.coveragePeriod().trim() : null);
        report.setForecastResult(result);
        report.setInsights(request.insights());
        report.setStatus(StringUtils.hasText(request.status()) ? request.status().trim() : "PUBLISHED");
        report.setPublishedAt(request.publishedAt() != null ? request.publishedAt() : LocalDateTime.now());
        report.setAutoGenerated(Boolean.TRUE.equals(request.autoGenerated()));
        return reportRepository.save(report);
    }

    @Override
    @Transactional
    public ReportDetailResponse generate(ReportGenerationRequest request) {
        Region region = regionRepository.findById(request.regionId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "分析区域不存在"));
        Crop crop = cropRepository.findById(request.cropId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "农作物不存在"));

        List<YieldRecord> historicalRecords = yieldRecordRepository.findByRegionIdAndCropIdOrderByYearAsc(region.getId(), crop.getId());
        if (CollectionUtils.isEmpty(historicalRecords)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所选条件暂无历史产量数据，无法生成报告");
        }

        int minYear = historicalRecords.stream()
            .map(YieldRecord::getYear)
            .filter(Objects::nonNull)
            .min(Integer::compareTo)
            .orElseThrow(() -> new BusinessException(ResultCode.SERVER_ERROR, "历史数据缺失年份信息"));
        int maxYear = historicalRecords.stream()
            .map(YieldRecord::getYear)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElseThrow(() -> new BusinessException(ResultCode.SERVER_ERROR, "历史数据缺失年份信息"));

        int requestedStart = request.startYear() != null ? request.startYear() : minYear;
        int requestedEnd = request.endYear() != null ? request.endYear() : maxYear;
        if (requestedStart > requestedEnd) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始年份不能晚于结束年份");
        }

        int startYear = Math.max(minYear, requestedStart);
        int endYear = Math.min(maxYear, requestedEnd);

        List<YieldRecord> filteredRecords = historicalRecords.stream()
            .filter(record -> record.getYear() != null)
            .filter(record -> record.getYear() >= startYear && record.getYear() <= endYear)
            .sorted(Comparator.comparing(YieldRecord::getYear))
            .toList();

        if (filteredRecords.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所选年份范围内缺少产量数据");
        }

        DoubleSummaryStatistics yieldStats = filteredRecords.stream()
            .map(YieldRecord::getYieldPerHectare)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();

        DoubleSummaryStatistics productionStats = filteredRecords.stream()
            .map(YieldRecord::getProduction)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();

        DoubleSummaryStatistics sownAreaStats = filteredRecords.stream()
            .map(YieldRecord::getSownArea)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();

        YieldRecord latestRecord = filteredRecords.get(filteredRecords.size() - 1);
        YieldRecord previousRecord = filteredRecords.size() > 1 ? filteredRecords.get(filteredRecords.size() - 2) : null;
        Double yieldYoY = computeYoY(latestRecord.getYieldPerHectare(), previousRecord != null ? previousRecord.getYieldPerHectare() : null);
        Double productionYoY = computeYoY(latestRecord.getProduction(), previousRecord != null ? previousRecord.getProduction() : null);

        Optional<YieldRecord> bestYieldRecord = filteredRecords.stream()
            .filter(record -> record.getYieldPerHectare() != null)
            .max(Comparator.comparing(YieldRecord::getYieldPerHectare));

        ForecastResult forecastResult = resolveForecastResult(request, crop.getId(), region.getId());
        Double actualYieldForForecastYear = null;
        if (forecastResult != null) {
            actualYieldForForecastYear = yieldRecordRepository.findByCropIdAndRegionIdAndYear(crop.getId(), region.getId(), forecastResult.getTargetYear())
                .map(YieldRecord::getYieldPerHectare)
                .orElse(null);
        }

        String title = StringUtils.hasText(request.title())
            ? request.title().trim()
            : String.format(Locale.CHINA, "%s%s产量分析报告", region.getName(), crop.getName());
        String coverage = startYear == endYear
            ? String.format(Locale.CHINA, "%d年", startYear)
            : String.format(Locale.CHINA, "%d-%d年", startYear, endYear);
        String description = StringUtils.hasText(request.description())
            ? request.description().trim()
            : String.format(Locale.CHINA, "%s%s %s 农情与市场研判", region.getName(), crop.getName(), coverage);

        List<String> highlights = buildHighlights(yieldStats, productionStats, yieldYoY, productionYoY, bestYieldRecord.orElse(null),
            forecastResult, actualYieldForForecastYear);
        String insights = highlights.isEmpty() ? null : String.join("；", highlights);

        Report report = new Report();
        report.setTitle(title);
        report.setDescription(description);
        report.setAuthor(StringUtils.hasText(request.author()) ? request.author().trim() : "系统自动");
        report.setCoveragePeriod(coverage);
        report.setForecastResult(forecastResult);
        report.setInsights(insights);
        report.setStatus("PUBLISHED");
        report.setPublishedAt(LocalDateTime.now());
        report.setAutoGenerated(true);

        int order = 1;
        report.addSection(buildSection("OVERVIEW", "核心指标概览", "历史产量与关键指标一览",
            buildOverviewData(region, crop, startYear, endYear, filteredRecords, yieldStats, productionStats, sownAreaStats,
                yieldYoY, productionYoY, highlights), order++));

        report.addSection(buildSection("YIELD_TREND", "历年产量趋势", "按年度展示播种面积、总产量与单产变化",
            buildYieldTrendData(filteredRecords), order++));

        if (forecastResult != null) {
            report.addSection(buildSection("FORECAST_COMPARISON", "预测结果对比", "对比预测与实际表现，评估模型效果",
                buildForecastComparisonData(forecastResult, actualYieldForForecastYear), order));
        }

        Report saved = reportRepository.save(report);
        return getDetail(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getDetail(Long id) {
        Report report = reportRepository.findWithSectionsById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "报告不存在"));
        return toDetail(report);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportExportResult export(Long id, String format) {
        ReportDetailResponse detail = getDetail(id);
        String normalized = StringUtils.hasText(format) ? format.trim().toLowerCase(Locale.ROOT) : "pdf";
        return switch (normalized) {
            case "excel", "xlsx" -> new ReportExportResult(
                exportAsExcel(detail),
                buildFilename(detail.summary().title(), "xlsx"),
                EXCEL_MEDIA_TYPE
            );
            case "pdf" -> new ReportExportResult(
                exportAsPdf(detail),
                buildFilename(detail.summary().title(), "pdf"),
                MediaType.APPLICATION_PDF
            );
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "暂不支持的导出格式: " + format);
        };
    }

    private ReportDetailResponse toDetail(Report report) {
        List<ReportSectionResponse> sections = report.getSections().stream()
            .sorted(Comparator.comparing(ReportSection::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ReportSection::getId, Comparator.nullsLast(Long::compareTo)))
            .map(this::toSectionResponse)
            .toList();
        return new ReportDetailResponse(toSummary(report), sections);
    }

    private byte[] exportAsPdf(ReportDetailResponse detail) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont baseFont = BaseFont.createFont(DEFAULT_FONT, "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font headerFont = new Font(baseFont, 12, Font.BOLD);
            Font bodyFont = new Font(baseFont, 10);

            Paragraph title = new Paragraph("报告导出：" + detail.summary().title(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("导出时间：" + DATE_FORMATTER.format(LocalDate.now()), bodyFont));
            document.add(new Paragraph("作者：" + safeText(detail.summary().author()), bodyFont));
            document.add(new Paragraph("覆盖周期：" + safeText(detail.summary().coveragePeriod()), bodyFont));
            document.add(new Paragraph("状态：" + safeText(detail.summary().status()), bodyFont));
            if (detail.summary().insights() != null) {
                document.add(new Paragraph("洞察：" + detail.summary().insights(), bodyFont));
            }
            document.add(new Paragraph(" "));

            for (ReportSectionResponse section : detail.sections()) {
                Paragraph sectionTitle = new Paragraph(section.title(), headerFont);
                sectionTitle.setSpacingBefore(6f);
                document.add(sectionTitle);
                if (StringUtils.hasText(section.description())) {
                    document.add(new Paragraph(section.description(), bodyFont));
                }

                if ("OVERVIEW".equalsIgnoreCase(section.type())) {
                    PdfPTable metricTable = new PdfPTable(3);
                    metricTable.setWidthPercentage(100);
                    addHeaderCell(metricTable, "指标", headerFont);
                    addHeaderCell(metricTable, "数值", headerFont);
                    addHeaderCell(metricTable, "单位", headerFont);
                    JsonNode metrics = section.data() != null ? section.data().get("metrics") : null;
                    if (metrics != null && metrics.isArray()) {
                        metrics.forEach(node -> {
                            addBodyCell(metricTable, safeText(node.get("label")), bodyFont);
                            addBodyCell(metricTable, formatNullableNumber(node.get("value")), bodyFont);
                            addBodyCell(metricTable, safeText(node.get("unit")), bodyFont);
                        });
                    }
                    document.add(metricTable);

                    JsonNode highlights = section.data() != null ? section.data().get("highlights") : null;
                    if (highlights != null && highlights.isArray() && highlights.size() > 0) {
                        document.add(new Paragraph("重点洞察：", bodyFont));
                        highlights.forEach(node -> {
                            document.add(new Paragraph("• " + node.asText(), bodyFont));
                        });
                    }
                } else if ("YIELD_TREND".equalsIgnoreCase(section.type())) {
                    PdfPTable table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    addHeaderCell(table, "年份", headerFont);
                    addHeaderCell(table, "播种面积(千公顷)", headerFont);
                    addHeaderCell(table, "总产量(万吨)", headerFont);
                    addHeaderCell(table, "单产(吨/公顷)", headerFont);
                    JsonNode series = section.data() != null ? section.data().get("series") : null;
                    if (series != null && series.isArray()) {
                        series.forEach(node -> {
                            addBodyCell(table, formatNullableNumber(node.get("year")), bodyFont);
                            addBodyCell(table, formatNullableNumber(node.get("sownArea")), bodyFont);
                            addBodyCell(table, formatNullableNumber(node.get("production")), bodyFont);
                            addBodyCell(table, formatNullableNumber(node.get("yieldPerHectare")), bodyFont);
                        });
                    }
                    document.add(table);
                } else if ("FORECAST_COMPARISON".equalsIgnoreCase(section.type())) {
                    PdfPTable table = new PdfPTable(3);
                    table.setWidthPercentage(100);
                    addHeaderCell(table, "指标", headerFont);
                    addHeaderCell(table, "数值", headerFont);
                    addHeaderCell(table, "单位", headerFont);
                    JsonNode data = section.data() != null ? section.data() : objectMapper.createObjectNode();
                    addBodyCell(table, "预测年份", bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("forecastYear")), bodyFont);
                    addBodyCell(table, "", bodyFont);

                    addBodyCell(table, "预测单产", bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("predictedYield")), bodyFont);
                    addBodyCell(table, "吨/公顷", bodyFont);

                    addBodyCell(table, "预测总产量", bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("predictedProduction")), bodyFont);
                    addBodyCell(table, "万吨", bodyFont);

                    String measurementLabel = safeText(data.get("measurementLabel"));
                    String measurementUnit = safeText(data.get("measurementUnit"));
                    addBodyCell(table, StringUtils.hasText(measurementLabel) ? measurementLabel : "测量值", bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("measurementValue")), bodyFont);
                    addBodyCell(table, measurementUnit, bodyFont);

                    addBodyCell(table, "实际单产", bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("actualYield")), bodyFont);
                    addBodyCell(table, "吨/公顷", bodyFont);

                    addBodyCell(table, "预测与实际差值", bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("difference")), bodyFont);
                    addBodyCell(table, "吨/公顷", bodyFont);

                    addBodyCell(table, "模型评价", bodyFont);
                    addBodyCell(table, safeText(data.get("evaluation")), bodyFont);
                    addBodyCell(table, "", bodyFont);

                    document.add(table);

                    JsonNode task = data.get("task");
                    if (task != null && task.isObject()) {
                        Paragraph taskTitle = new Paragraph("任务信息", headerFont);
                        taskTitle.setSpacingBefore(6f);
                        document.add(taskTitle);

                        PdfPTable taskTable = new PdfPTable(2);
                        taskTable.setWidthPercentage(100);
                        addHeaderCell(taskTable, "字段", headerFont);
                        addHeaderCell(taskTable, "值", headerFont);
                        addBodyCell(taskTable, "任务ID", bodyFont);
                        addBodyCell(taskTable, safeText(task.get("taskId")), bodyFont);
                        addBodyCell(taskTable, "模型", bodyFont);
                        addBodyCell(taskTable, safeText(task.get("model")), bodyFont);
                        addBodyCell(taskTable, "模型类型", bodyFont);
                        addBodyCell(taskTable, safeText(task.get("modelType")), bodyFont);
                        addBodyCell(taskTable, "区域", bodyFont);
                        addBodyCell(taskTable, safeText(task.get("region")), bodyFont);
                        addBodyCell(taskTable, "作物", bodyFont);
                        addBodyCell(taskTable, safeText(task.get("crop")), bodyFont);
                        document.add(taskTable);
                    }
                } else {
                    document.add(new Paragraph(toPrettyJson(section.data()), bodyFont));
                }

                document.add(new Paragraph(" "));
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("导出 PDF 失败", exception);
        }
    }

    private byte[] exportAsExcel(ReportDetailResponse detail) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("报告详情");
            CreationHelper creationHelper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));

            int rowIndex = 0;
            rowIndex = writeSummaryRows(detail, sheet, dateStyle, rowIndex);
            rowIndex += 1;

            for (ReportSectionResponse section : detail.sections()) {
                Row sectionHeader = sheet.createRow(rowIndex++);
                sectionHeader.createCell(0).setCellValue(section.title());
                sectionHeader.createCell(1).setCellValue(section.type());
                if (StringUtils.hasText(section.description())) {
                    sectionHeader.createCell(2).setCellValue(section.description());
                }

                if ("OVERVIEW".equalsIgnoreCase(section.type())) {
                    Row headerRow = sheet.createRow(rowIndex++);
                    headerRow.createCell(0).setCellValue("指标");
                    headerRow.createCell(1).setCellValue("数值");
                    headerRow.createCell(2).setCellValue("单位");
                    JsonNode metrics = section.data() != null ? section.data().get("metrics") : null;
                    if (metrics != null && metrics.isArray()) {
                        for (JsonNode metric : metrics) {
                            Row row = sheet.createRow(rowIndex++);
                            row.createCell(0).setCellValue(safeText(metric.get("label")));
                            setNumeric(row, 1, metric.get("value"));
                            row.createCell(2).setCellValue(safeText(metric.get("unit")));
                        }
                    }
                    JsonNode highlights = section.data() != null ? section.data().get("highlights") : null;
                    if (highlights != null && highlights.isArray() && highlights.size() > 0) {
                        Row highlightHeader = sheet.createRow(rowIndex++);
                        highlightHeader.createCell(0).setCellValue("重点洞察");
                        for (JsonNode node : highlights) {
                            sheet.createRow(rowIndex++)
                                .createCell(0)
                                .setCellValue(node.asText());
                        }
                    }
                } else if ("YIELD_TREND".equalsIgnoreCase(section.type())) {
                    Row headerRow = sheet.createRow(rowIndex++);
                    headerRow.createCell(0).setCellValue("年份");
                    headerRow.createCell(1).setCellValue("播种面积(千公顷)");
                    headerRow.createCell(2).setCellValue("总产量(万吨)");
                    headerRow.createCell(3).setCellValue("单产(吨/公顷)");
                    JsonNode series = section.data() != null ? section.data().get("series") : null;
                    if (series != null && series.isArray()) {
                        for (JsonNode node : series) {
                            Row row = sheet.createRow(rowIndex++);
                            setNumeric(row, 0, node.get("year"));
                            setNumeric(row, 1, node.get("sownArea"));
                            setNumeric(row, 2, node.get("production"));
                            setNumeric(row, 3, node.get("yieldPerHectare"));
                        }
                    }
                } else if ("FORECAST_COMPARISON".equalsIgnoreCase(section.type())) {
                    Row headerRow = sheet.createRow(rowIndex++);
                    headerRow.createCell(0).setCellValue("指标");
                    headerRow.createCell(1).setCellValue("数值");
                    headerRow.createCell(2).setCellValue("单位");
                    JsonNode data = section.data() != null ? section.data() : objectMapper.createObjectNode();

                    Row forecastYearRow = sheet.createRow(rowIndex++);
                    forecastYearRow.createCell(0).setCellValue("预测年份");
                    setNumeric(forecastYearRow, 1, data.get("forecastYear"));

                    Row predictedYieldRow = sheet.createRow(rowIndex++);
                    predictedYieldRow.createCell(0).setCellValue("预测单产");
                    setNumeric(predictedYieldRow, 1, data.get("predictedYield"));
                    predictedYieldRow.createCell(2).setCellValue("吨/公顷");

                    Row predictedProductionRow = sheet.createRow(rowIndex++);
                    predictedProductionRow.createCell(0).setCellValue("预测总产量");
                    setNumeric(predictedProductionRow, 1, data.get("predictedProduction"));
                    predictedProductionRow.createCell(2).setCellValue("万吨");

                    String measurementLabel = safeText(data.get("measurementLabel"));
                    Row measurementRow = sheet.createRow(rowIndex++);
                    measurementRow.createCell(0).setCellValue(StringUtils.hasText(measurementLabel) ? measurementLabel : "测量值");
                    setNumeric(measurementRow, 1, data.get("measurementValue"));
                    measurementRow.createCell(2).setCellValue(safeText(data.get("measurementUnit")));

                    Row actualYieldRow = sheet.createRow(rowIndex++);
                    actualYieldRow.createCell(0).setCellValue("实际单产");
                    setNumeric(actualYieldRow, 1, data.get("actualYield"));
                    actualYieldRow.createCell(2).setCellValue("吨/公顷");

                    Row differenceRow = sheet.createRow(rowIndex++);
                    differenceRow.createCell(0).setCellValue("预测与实际差值");
                    setNumeric(differenceRow, 1, data.get("difference"));
                    differenceRow.createCell(2).setCellValue("吨/公顷");

                    Row evaluationRow = sheet.createRow(rowIndex++);
                    evaluationRow.createCell(0).setCellValue("模型评价");
                    evaluationRow.createCell(1).setCellValue(safeText(data.get("evaluation")));

                    JsonNode task = data.get("task");
                    if (task != null && task.isObject()) {
                        rowIndex += 1;
                        Row taskHeader = sheet.createRow(rowIndex++);
                        taskHeader.createCell(0).setCellValue("任务信息");
                        taskHeader.createCell(1).setCellValue("值");

                        Row taskIdRow = sheet.createRow(rowIndex++);
                        taskIdRow.createCell(0).setCellValue("任务ID");
                        taskIdRow.createCell(1).setCellValue(safeText(task.get("taskId")));

                        Row modelRow = sheet.createRow(rowIndex++);
                        modelRow.createCell(0).setCellValue("模型");
                        modelRow.createCell(1).setCellValue(safeText(task.get("model")));

                        Row modelTypeRow = sheet.createRow(rowIndex++);
                        modelTypeRow.createCell(0).setCellValue("模型类型");
                        modelTypeRow.createCell(1).setCellValue(safeText(task.get("modelType")));

                        Row regionRow = sheet.createRow(rowIndex++);
                        regionRow.createCell(0).setCellValue("区域");
                        regionRow.createCell(1).setCellValue(safeText(task.get("region")));

                        Row cropRow = sheet.createRow(rowIndex++);
                        cropRow.createCell(0).setCellValue("作物");
                        cropRow.createCell(1).setCellValue(safeText(task.get("crop")));
                    }
                } else {
                    sheet.createRow(rowIndex++).createCell(0).setCellValue(toPrettyJson(section.data()));
                }

                rowIndex += 1;
            }

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("导出 Excel 失败", exception);
        }
    }

    private ReportSummaryResponse toSummary(Report report) {
        return new ReportSummaryResponse(
            report.getId(),
            report.getTitle(),
            report.getDescription(),
            report.getAuthor(),
            report.getCoveragePeriod(),
            report.getStatus(),
            report.isAutoGenerated(),
            report.getPublishedAt(),
            report.getInsights()
        );
    }

    private int writeSummaryRows(ReportDetailResponse detail, Sheet sheet, CellStyle dateStyle, int rowIndex) {
        Row titleRow = sheet.createRow(rowIndex++);
        titleRow.createCell(0).setCellValue("报告标题");
        titleRow.createCell(1).setCellValue(detail.summary().title());

        Row coverageRow = sheet.createRow(rowIndex++);
        coverageRow.createCell(0).setCellValue("覆盖周期");
        coverageRow.createCell(1).setCellValue(safeText(detail.summary().coveragePeriod()));

        Row authorRow = sheet.createRow(rowIndex++);
        authorRow.createCell(0).setCellValue("作者");
        authorRow.createCell(1).setCellValue(safeText(detail.summary().author()));

        Row statusRow = sheet.createRow(rowIndex++);
        statusRow.createCell(0).setCellValue("状态");
        statusRow.createCell(1).setCellValue(safeText(detail.summary().status()));

        if (detail.summary().publishedAt() != null) {
            Row publishRow = sheet.createRow(rowIndex++);
            publishRow.createCell(0).setCellValue("发布时间");
            Cell dateCell = publishRow.createCell(1);
            dateCell.setCellValue(java.sql.Timestamp.valueOf(detail.summary().publishedAt()));
            dateCell.setCellStyle(dateStyle);
        }

        if (StringUtils.hasText(detail.summary().insights())) {
            Row insightRow = sheet.createRow(rowIndex++);
            insightRow.createCell(0).setCellValue("洞察");
            insightRow.createCell(1).setCellValue(detail.summary().insights());
        }
        return rowIndex;
    }

    private ReportSection buildSection(String type, String title, String description, Object data, int sortOrder) {
        ReportSection section = new ReportSection();
        section.setType(type);
        section.setTitle(title);
        section.setDescription(description);
        section.setSortOrder(sortOrder);
        section.setData(toJsonString(data));
        return section;
    }

    private ReportSectionResponse toSectionResponse(ReportSection section) {
        return new ReportSectionResponse(
            section.getId(),
            section.getType(),
            section.getTitle(),
            section.getDescription(),
            toJsonNode(section.getData()),
            section.getSortOrder()
        );
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

    private void setNumeric(Row row, int columnIndex, JsonNode value) {
        if (value == null || value.isNull()) {
            row.createCell(columnIndex).setBlank();
            return;
        }
        if (value.isNumber()) {
            row.createCell(columnIndex).setCellValue(value.asDouble());
            return;
        }
        row.createCell(columnIndex).setCellValue(value.asText());
    }

    private String formatNullableNumber(JsonNode value) {
        if (value == null || value.isNull()) {
            return "-";
        }
        if (value.isIntegralNumber()) {
            return value.asText();
        }
        if (value.isNumber()) {
            return String.format(Locale.CHINA, "%.2f", value.asDouble());
        }
        return value.asText();
    }

    private String toPrettyJson(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return "{}";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return node.toString();
        }
    }

    private String safeText(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String buildFilename(String title, String extension) {
        String base = StringUtils.hasText(title) ? title : "报告导出";
        String sanitized = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        return sanitized + "_" + LocalDate.now() + "." + extension;
    }

    private String toJsonString(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "生成报告内容时出现异常");
        }
    }

    private JsonNode toJsonNode(String json) {
        if (!StringUtils.hasText(json)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("raw", json);
            return fallback;
        }
    }

    private Double computeYoY(Double current, Double previous) {
        if (current == null || previous == null || Math.abs(previous) < 1e-6) {
            return null;
        }
        return (current - previous) / previous * 100.0;
    }

    private Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value)
            .setScale(scale, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private Map<String, Object> buildOverviewData(Region region,
                                                  Crop crop,
                                                  int startYear,
                                                  int endYear,
                                                  List<YieldRecord> records,
                                                  DoubleSummaryStatistics yieldStats,
                                                  DoubleSummaryStatistics productionStats,
                                                  DoubleSummaryStatistics sownAreaStats,
                                                  Double yieldYoY,
                                                  Double productionYoY,
                                                  List<String> highlights) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("region", region.getName());
        data.put("crop", crop.getName());
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("start", startYear);
        range.put("end", endYear);
        range.put("years", records.size());
        data.put("yearRange", range);

        List<Map<String, Object>> metrics = new ArrayList<>();
        if (yieldStats.getCount() > 0) {
            metrics.add(metric("平均单产", round(yieldStats.getAverage(), 2), "吨/公顷"));
        }
        if (productionStats.getCount() > 0) {
            metrics.add(metric("平均总产量", round(productionStats.getAverage(), 2), "万吨"));
        }
        if (sownAreaStats.getCount() > 0) {
            metrics.add(metric("平均播种面积", round(sownAreaStats.getAverage(), 2), "千公顷"));
        }
        if (yieldYoY != null) {
            metrics.add(metric("单产同比", round(yieldYoY, 2), "%"));
        }
        if (productionYoY != null) {
            metrics.add(metric("总产同比", round(productionYoY, 2), "%"));
        }
        data.put("metrics", metrics);
        data.put("highlights", highlights);
        return data;
    }

    private Map<String, Object> metric(String label, Double value, String unit) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("label", label);
        metric.put("value", value);
        metric.put("unit", unit);
        return metric;
    }

    private Map<String, Object> buildYieldTrendData(List<YieldRecord> records) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> series = records.stream()
            .map(record -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("year", record.getYear());
                entry.put("yieldPerHectare", record.getYieldPerHectare());
                entry.put("production", record.getProduction());
                entry.put("sownArea", record.getSownArea());
                return entry;
            })
            .toList();
        data.put("series", series);
        return data;
    }

    private Map<String, Object> buildForecastComparisonData(ForecastResult forecastResult, Double actualYield) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("forecastYear", forecastResult.getTargetYear());
        data.put("predictedYield", forecastResult.getPredictedYield());
        data.put("predictedProduction", forecastResult.getPredictedProduction());
        data.put("measurementValue", forecastResult.getMeasurementValue());
        data.put("measurementLabel", forecastResult.getMeasurementLabel());
        data.put("measurementUnit", forecastResult.getMeasurementUnit());
        data.put("evaluation", forecastResult.getEvaluation());
        data.put("actualYield", actualYield);
        Double difference = null;
        if (actualYield != null && forecastResult.getPredictedYield() != null) {
            difference = round(forecastResult.getPredictedYield() - actualYield, 2);
        }
        data.put("difference", difference);
        Map<String, Object> taskInfo = new LinkedHashMap<>();
        taskInfo.put("taskId", forecastResult.getTask().getId());
        taskInfo.put("model", forecastResult.getTask().getModel().getName());
        taskInfo.put("modelType", forecastResult.getTask().getModel().getType().name());
        taskInfo.put("region", forecastResult.getTask().getRegion().getName());
        taskInfo.put("crop", forecastResult.getTask().getCrop().getName());
        data.put("task", taskInfo);
        return data;
    }

    private List<String> buildHighlights(DoubleSummaryStatistics yieldStats,
                                         DoubleSummaryStatistics productionStats,
                                         Double yieldYoY,
                                         Double productionYoY,
                                         YieldRecord bestYieldRecord,
                                         ForecastResult forecastResult,
                                         Double actualYieldForForecastYear) {
        List<String> highlights = new ArrayList<>();
        if (yieldStats.getCount() > 0) {
            highlights.add(String.format(Locale.CHINA, "平均单产 %.2f 吨/公顷", yieldStats.getAverage()));
        }
        if (productionStats.getCount() > 0) {
            highlights.add(String.format(Locale.CHINA, "平均总产量 %.2f 万吨", productionStats.getAverage()));
        }
        if (yieldYoY != null) {
            highlights.add(String.format(Locale.CHINA, "最近一年单产同比%s%.2f%%", yieldYoY >= 0 ? "增长" : "下降", Math.abs(yieldYoY)));
        }
        if (productionYoY != null) {
            highlights.add(String.format(Locale.CHINA, "最近一年总产量同比%s%.2f%%", productionYoY >= 0 ? "增长" : "下降", Math.abs(productionYoY)));
        }
        if (bestYieldRecord != null && bestYieldRecord.getYieldPerHectare() != null) {
            highlights.add(String.format(Locale.CHINA, "%d 年达到最高单产 %.2f 吨/公顷", bestYieldRecord.getYear(), bestYieldRecord.getYieldPerHectare()));
        }
        if (forecastResult != null) {
            Double predictedYield = forecastResult.getPredictedYield();
            if (predictedYield != null) {
                if (actualYieldForForecastYear != null) {
                    double diff = predictedYield - actualYieldForForecastYear;
                    highlights.add(String.format(Locale.CHINA, "预测 %d 年单产 %.2f 吨/公顷，较实际偏差 %.2f 吨/公顷",
                        forecastResult.getTargetYear(), predictedYield, diff));
                } else {
                    highlights.add(String.format(Locale.CHINA, "预测 %d 年单产 %.2f 吨/公顷，建议关注后续实际表现",
                        forecastResult.getTargetYear(), predictedYield));
                }
            } else if (forecastResult.getMeasurementValue() != null) {
                String label = StringUtils.hasText(forecastResult.getMeasurementLabel())
                    ? forecastResult.getMeasurementLabel()
                    : "预测值";
                String unit = StringUtils.hasText(forecastResult.getMeasurementUnit())
                    ? " " + forecastResult.getMeasurementUnit()
                    : "";
                highlights.add(String.format(Locale.CHINA, "预测 %d 年%s %.2f%s，建议关注后续实际表现",
                    forecastResult.getTargetYear(), label, forecastResult.getMeasurementValue(), unit));
            }
        }
        return highlights;
    }

    private ForecastResult resolveForecastResult(ReportGenerationRequest request, Long cropId, Long regionId) {
        if (!Boolean.TRUE.equals(request.includeForecastComparison()) || request.forecastResultId() == null) {
            return null;
        }
        ForecastResult result = forecastResultRepository.findById(request.forecastResultId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "预测结果不存在"));
        if (!Objects.equals(result.getTask().getCrop().getId(), cropId)
            || !Objects.equals(result.getTask().getRegion().getId(), regionId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预测结果与所选区域或作物不匹配");
        }
        return result;
    }
}
