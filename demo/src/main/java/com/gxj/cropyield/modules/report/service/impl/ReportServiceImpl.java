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
    private static final String DEFAULT_FONT = "STSong-Light";

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

            Paragraph title = new Paragraph(sanitizeForPdf("报告导出：" + detail.summary().title()), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(sanitizeForPdf("导出时间：" + DATE_FORMATTER.format(LocalDate.now())), bodyFont));
            document.add(new Paragraph(sanitizeForPdf("作者：" + safeText(detail.summary().author())), bodyFont));
            document.add(new Paragraph(sanitizeForPdf("覆盖周期：" + safeText(detail.summary().coveragePeriod())), bodyFont));
            document.add(new Paragraph(sanitizeForPdf("状态：" + safeText(detail.summary().status())), bodyFont));
            if (detail.summary().insights() != null) {
                document.add(new Paragraph(sanitizeForPdf("洞察：" + detail.summary().insights()), bodyFont));
            }
            document.add(new Paragraph(" "));

            for (ReportSectionResponse section : detail.sections()) {
                Paragraph sectionTitle = new Paragraph(sanitizeForPdf(section.title()), headerFont);
                sectionTitle.setSpacingBefore(6f);
                document.add(sectionTitle);
                if (StringUtils.hasText(section.description())) {
                    document.add(new Paragraph(sanitizeForPdf(section.description()), bodyFont));
                }

                if ("OVERVIEW".equalsIgnoreCase(section.type())) {
                    PdfPTable metricTable = new PdfPTable(3);
                    metricTable.setWidthPercentage(100);
                    addHeaderCell(metricTable, sanitizeForPdf("指标"), headerFont);
                    addHeaderCell(metricTable, sanitizeForPdf("数值"), headerFont);
                    addHeaderCell(metricTable, sanitizeForPdf("单位"), headerFont);
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
                        document.add(new Paragraph(sanitizeForPdf("重点洞察："), bodyFont));
                        highlights.forEach(node -> {
                            document.add(new Paragraph(sanitizeForPdf("• " + node.asText()), bodyFont));
                        });
                    }
                } else if ("YIELD_TREND".equalsIgnoreCase(section.type())) {
                    PdfPTable table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    addHeaderCell(table, sanitizeForPdf("年份"), headerFont);
                    addHeaderCell(table, sanitizeForPdf("播种面积(千公顷)"), headerFont);
                    addHeaderCell(table, sanitizeForPdf("总产量(万吨)"), headerFont);
                    addHeaderCell(table, sanitizeForPdf("单产(吨/公顷)"), headerFont);
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
                    addHeaderCell(table, sanitizeForPdf("指标"), headerFont);
                    addHeaderCell(table, sanitizeForPdf("数值"), headerFont);
                    addHeaderCell(table, sanitizeForPdf("单位"), headerFont);
                    JsonNode data = section.data() != null ? section.data() : objectMapper.createObjectNode();
                    addBodyCell(table, sanitizeForPdf("预测年份"), bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("forecastYear")), bodyFont);
                    addBodyCell(table, "", bodyFont);

                    addBodyCell(table, sanitizeForPdf("预测单产"), bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("predictedYield")), bodyFont);
                    addBodyCell(table, sanitizeForPdf("吨/公顷"), bodyFont);

                    addBodyCell(table, sanitizeForPdf("预测总产量"), bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("predictedProduction")), bodyFont);
                    addBodyCell(table, sanitizeForPdf("万吨"), bodyFont);

                    String measurementLabel = safeText(data.get("measurementLabel"));
                    String measurementUnit = safeText(data.get("measurementUnit"));
                    addBodyCell(table, StringUtils.hasText(measurementLabel) ? measurementLabel : sanitizeForPdf("测量值"), bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("measurementValue")), bodyFont);
                    addBodyCell(table, measurementUnit, bodyFont);

                    addBodyCell(table, sanitizeForPdf("实际单产"), bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("actualYield")), bodyFont);
                    addBodyCell(table, sanitizeForPdf("吨/公顷"), bodyFont);

                    addBodyCell(table, sanitizeForPdf("预测与实际差值"), bodyFont);
                    addBodyCell(table, formatNullableNumber(data.get("difference")), bodyFont);
                    addBodyCell(table, sanitizeForPdf("吨/公顷"), bodyFont);

                    addBodyCell(table, sanitizeForPdf("模型评价"), bodyFont);
                    addBodyCell(table, safeText(data.get("evaluation")), bodyFont);
                    addBodyCell(table, "", bodyFont);

                    document.add(table);

                    JsonNode task = data.get("task");
                    if (task != null && task.isObject()) {
                        Paragraph taskTitle = new Paragraph(sanitizeForPdf("任务信息"), headerFont);
                        taskTitle.setSpacingBefore(6f);
                        document.add(taskTitle);

                        PdfPTable taskTable = new PdfPTable(2);
                        taskTable.setWidthPercentage(100);
                        addHeaderCell(taskTable, sanitizeForPdf("字段"), headerFont);
                        addHeaderCell(taskTable, sanitizeForPdf("值"), headerFont);
                        addBodyCell(taskTable, sanitizeForPdf("任务ID"), bodyFont);
                        addBodyCell(taskTable, safeText(task.get("taskId")), bodyFont);
                        addBodyCell(taskTable, sanitizeForPdf("模型"), bodyFont);
                        addBodyCell(taskTable, safeText(task.get("model")), bodyFont);
                        addBodyCell(taskTable, sanitizeForPdf("模型类型"), bodyFont);
                        addBodyCell(taskTable, safeText(task.get("modelType")), bodyFont);
                        addBodyCell(taskTable, sanitizeForPdf("区域"), bodyFont);
                        addBodyCell(taskTable, safeText(task.get("region")), bodyFont);
                        addBodyCell(taskTable, sanitizeForPdf("作物"), bodyFont);
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
            
            // 创建样式
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            
            CellStyle sectionHeaderStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font sectionFont = workbook.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 14);
            sectionHeaderStyle.setFont(sectionFont);
            sectionHeaderStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            sectionHeaderStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));
            
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(creationHelper.createDataFormat().getFormat("0.00"));

            int rowIndex = 0;
            
            // 标题行
            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(detail.summary().title());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));
            
            rowIndex++; // 空行
            
            // 报告摘要信息
            rowIndex = writeSummaryRows(detail, sheet, dateStyle, rowIndex);
            rowIndex += 1; // 空行

            for (ReportSectionResponse section : detail.sections()) {
                // 章节标题
                Row sectionTitleRow = sheet.createRow(rowIndex++);
                Cell sectionTitleCell = sectionTitleRow.createCell(0);
                sectionTitleCell.setCellValue(section.title() + " (" + formatSectionType(section.type()) + ")");
                sectionTitleCell.setCellStyle(sectionHeaderStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                    rowIndex - 1, rowIndex - 1, 0, 5));
                
                if (StringUtils.hasText(section.description())) {
                    Row descRow = sheet.createRow(rowIndex++);
                    descRow.createCell(0).setCellValue(section.description());
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIndex - 1, rowIndex - 1, 0, 5));
                }

                if ("OVERVIEW".equalsIgnoreCase(section.type())) {
                    Row headerRow = sheet.createRow(rowIndex++);
                    Cell h1 = headerRow.createCell(0);
                    h1.setCellValue("指标");
                    h1.setCellStyle(headerStyle);
                    Cell h2 = headerRow.createCell(1);
                    h2.setCellValue("数值");
                    h2.setCellStyle(headerStyle);
                    Cell h3 = headerRow.createCell(2);
                    h3.setCellValue("单位");
                    h3.setCellStyle(headerStyle);
                    
                    JsonNode metrics = section.data() != null ? section.data().get("metrics") : null;
                    if (metrics != null && metrics.isArray()) {
                        for (JsonNode metric : metrics) {
                            Row row = sheet.createRow(rowIndex++);
                            row.createCell(0).setCellValue(safeText(metric.get("label")));
                            setNumericWithStyle(row, 1, metric.get("value"), numberStyle);
                            row.createCell(2).setCellValue(safeText(metric.get("unit")));
                        }
                    }
                    
                    JsonNode highlights = section.data() != null ? section.data().get("highlights") : null;
                    if (highlights != null && highlights.isArray() && highlights.size() > 0) {
                        rowIndex++; // 空行
                        Row highlightHeader = sheet.createRow(rowIndex++);
                        Cell highlightCell = highlightHeader.createCell(0);
                        highlightCell.setCellValue("重点洞察");
                        highlightCell.setCellStyle(headerStyle);
                        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                            rowIndex - 1, rowIndex - 1, 0, 2));
                        
                        for (JsonNode node : highlights) {
                            Row row = sheet.createRow(rowIndex++);
                            Cell cell = row.createCell(0);
                            cell.setCellValue("• " + node.asText());
                            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                                rowIndex - 1, rowIndex - 1, 0, 2));
                        }
                    }
                } else if ("YIELD_TREND".equalsIgnoreCase(section.type())) {
                    Row headerRow = sheet.createRow(rowIndex++);
                    String[] headers = {"年份", "播种面积(千公顷)", "总产量(万吨)", "单产(吨/公顷)"};
                    for (int i = 0; i < headers.length; i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(headerStyle);
                    }
                    
                    JsonNode series = section.data() != null ? section.data().get("series") : null;
                    if (series != null && series.isArray()) {
                        for (JsonNode node : series) {
                            Row row = sheet.createRow(rowIndex++);
                            setNumeric(row, 0, node.get("year"));
                            setNumericWithStyle(row, 1, node.get("sownArea"), numberStyle);
                            setNumericWithStyle(row, 2, node.get("production"), numberStyle);
                            setNumericWithStyle(row, 3, node.get("yieldPerHectare"), numberStyle);
                        }
                    }
                } else if ("FORECAST_COMPARISON".equalsIgnoreCase(section.type())) {
                    Row headerRow = sheet.createRow(rowIndex++);
                    String[] headers = {"指标", "数值", "单位"};
                    for (int i = 0; i < headers.length; i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(headerStyle);
                    }
                    
                    JsonNode data = section.data() != null ? section.data() : objectMapper.createObjectNode();

                    Row forecastYearRow = sheet.createRow(rowIndex++);
                    forecastYearRow.createCell(0).setCellValue("预测年份");
                    setNumeric(forecastYearRow, 1, data.get("forecastYear"));
                    forecastYearRow.createCell(2).setCellValue("");

                    Row predictedYieldRow = sheet.createRow(rowIndex++);
                    predictedYieldRow.createCell(0).setCellValue("预测单产");
                    setNumericWithStyle(predictedYieldRow, 1, data.get("predictedYield"), numberStyle);
                    predictedYieldRow.createCell(2).setCellValue("吨/公顷");

                    Row predictedProductionRow = sheet.createRow(rowIndex++);
                    predictedProductionRow.createCell(0).setCellValue("预测总产量");
                    setNumericWithStyle(predictedProductionRow, 1, data.get("predictedProduction"), numberStyle);
                    predictedProductionRow.createCell(2).setCellValue("万吨");

                    String measurementLabel = safeText(data.get("measurementLabel"));
                    Row measurementRow = sheet.createRow(rowIndex++);
                    measurementRow.createCell(0).setCellValue(StringUtils.hasText(measurementLabel) ? measurementLabel : "测量值");
                    setNumericWithStyle(measurementRow, 1, data.get("measurementValue"), numberStyle);
                    measurementRow.createCell(2).setCellValue(safeText(data.get("measurementUnit")));

                    Row actualYieldRow = sheet.createRow(rowIndex++);
                    actualYieldRow.createCell(0).setCellValue("实际单产");
                    setNumericWithStyle(actualYieldRow, 1, data.get("actualYield"), numberStyle);
                    actualYieldRow.createCell(2).setCellValue("吨/公顷");

                    Row differenceRow = sheet.createRow(rowIndex++);
                    differenceRow.createCell(0).setCellValue("预测与实际差值");
                    setNumericWithStyle(differenceRow, 1, data.get("difference"), numberStyle);
                    differenceRow.createCell(2).setCellValue("吨/公顷");

                    Row evaluationRow = sheet.createRow(rowIndex++);
                    evaluationRow.createCell(0).setCellValue("模型评价");
                    Cell evalCell = evaluationRow.createCell(1);
                    evalCell.setCellValue(safeText(data.get("evaluation")));
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIndex - 1, rowIndex - 1, 1, 2));

                    JsonNode task = data.get("task");
                    if (task != null && task.isObject()) {
                        rowIndex += 1; // 空行
                        Row taskHeader = sheet.createRow(rowIndex++);
                        Cell taskHeaderCell = taskHeader.createCell(0);
                        taskHeaderCell.setCellValue("任务信息");
                        taskHeaderCell.setCellStyle(headerStyle);
                        Cell taskHeaderCell2 = taskHeader.createCell(1);
                        taskHeaderCell2.setCellValue("值");
                        taskHeaderCell2.setCellStyle(headerStyle);

                        String[][] taskData = {
                            {"任务ID", safeText(task.get("taskId"))},
                            {"模型", safeText(task.get("model"))},
                            {"模型类型", safeText(task.get("modelType"))},
                            {"区域", safeText(task.get("region"))},
                            {"作物", safeText(task.get("crop"))}
                        };
                        
                        for (String[] pair : taskData) {
                            Row row = sheet.createRow(rowIndex++);
                            row.createCell(0).setCellValue(pair[0]);
                            row.createCell(1).setCellValue(pair[1]);
                        }
                    }
                } else {
                    Row dataRow = sheet.createRow(rowIndex++);
                    Cell dataCell = dataRow.createCell(0);
                    dataCell.setCellValue(toPrettyJson(section.data()));
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        rowIndex - 1, rowIndex - 1, 0, 5));
                }

                rowIndex += 1; // 章节间空行
            }

            // 自动调整列宽
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
                // 设置最小和最大列宽
                int currentWidth = sheet.getColumnWidth(i);
                if (currentWidth < 3000) {
                    sheet.setColumnWidth(i, 3000);
                } else if (currentWidth > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("导出 Excel 失败", exception);
        }
    }
    
    private void setNumericWithStyle(Row row, int columnIndex, JsonNode value, CellStyle numberStyle) {
        if (value == null || value.isNull()) {
            row.createCell(columnIndex).setBlank();
            return;
        }
        if (value.isNumber()) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value.asDouble());
            cell.setCellStyle(numberStyle);
            return;
        }
        row.createCell(columnIndex).setCellValue(value.asText());
    }
    
    private String formatSectionType(String type) {
        return switch (type) {
            case "OVERVIEW" -> "指标概览";
            case "YIELD_TREND" -> "产量趋势";
            case "FORECAST_COMPARISON" -> "预测对比";
            default -> type;
        };
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
        if (node == null || node.isNull()) {
            return "";
        }
        return sanitizeForPdf(node.asText());
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        return sanitizeForPdf(value);
    }
    
    private String sanitizeForPdf(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // 替换常见的特殊字符为 PDF 友好的版本
        return text
            // 数学符号
            .replace("²", "2")  // 上标2
            .replace("³", "3")  // 上标3
            .replace("±", "+/-")  // 正负号
            .replace("×", "x")  // 乘号
            .replace("÷", "/")  // 除号
            .replace("≈", "~")  // 约等于
            .replace("≤", "<=")  // 小于等于
            .replace("≥", ">=")  // 大于等于
            .replace("≠", "!=")  // 不等于
            .replace("≡", "===")  // 恒等于
            // 箭头符号
            .replace("→", "->")  // 箭头
            .replace("←", "<-")  // 左箭头
            .replace("↑", "^")  // 上箭头
            .replace("↓", "v")  // 下箭头
            .replace("↔", "<->")  // 双向箭头
            .replace("⇒", "=>")  // 蕴含
            .replace("⇔", "<=>")  // 等价
            // 希腊字母
            .replace("α", "alpha")
            .replace("β", "beta")
            .replace("γ", "gamma")
            .replace("δ", "delta")
            .replace("ε", "epsilon")
            .replace("ζ", "zeta")
            .replace("η", "eta")
            .replace("θ", "theta")
            .replace("ι", "iota")
            .replace("κ", "kappa")
            .replace("λ", "lambda")
            .replace("μ", "mu")
            .replace("ν", "nu")
            .replace("ξ", "xi")
            .replace("π", "pi")
            .replace("ρ", "rho")
            .replace("σ", "sigma")
            .replace("τ", "tau")
            .replace("υ", "upsilon")
            .replace("φ", "phi")
            .replace("χ", "chi")
            .replace("ψ", "psi")
            .replace("ω", "omega")
            .replace("Α", "Alpha")
            .replace("Β", "Beta")
            .replace("Γ", "Gamma")
            .replace("Δ", "Delta")
            .replace("Ε", "Epsilon")
            .replace("Ζ", "Zeta")
            .replace("Η", "Eta")
            .replace("Θ", "Theta")
            .replace("Ι", "Iota")
            .replace("Κ", "Kappa")
            .replace("Λ", "Lambda")
            .replace("Μ", "Mu")
            .replace("Ν", "Nu")
            .replace("Ξ", "Xi")
            .replace("Π", "Pi")
            .replace("Ρ", "Rho")
            .replace("Σ", "Sigma")
            .replace("Τ", "Tau")
            .replace("Υ", "Upsilon")
            .replace("Φ", "Phi")
            .replace("Χ", "Chi")
            .replace("Ψ", "Psi")
            .replace("Ω", "Omega")
            // 项目符号和形状
            .replace("•", "* ")  // 实心圆点
            .replace("◦", "o ")  // 空心圆
            .replace("▪", "* ")  // 实心方块
            .replace("▫", "o ")  // 空心方块
            .replace("■", "* ")  // 大实心方块
            .replace("□", "o ")  // 大空心方块
            .replace("▲", "^ ")  // 实心上三角
            .replace("△", "^ ")  // 空心上三角
            .replace("▼", "v ")  // 实心下三角
            .replace("▽", "v ")  // 空心下三角
            .replace("►", "> ")  // 实心右三角
            .replace("▻", "> ")  // 空心右三角
            .replace("◄", "< ")  // 实心左三角
            .replace("◅", "< ")  // 空心左三角
            .replace("◆", "* ")  // 实心菱形 - 这是你PDF中的符号
            .replace("◇", "o ")  // 空心菱形
            .replace("★", "* ")  // 实心星
            .replace("☆", "* ")  // 空心星
            .replace("●", "* ")  // 大实心圆
            .replace("○", "o ")  // 大空心圆
            .replace("◉", "* ")  // 双圆
            .replace("◎", "o ")  // 靶心
            // 货币符号
            .replace("€", "EUR ")
            .replace("£", "GBP ")
            .replace("¥", "CNY ")
            .replace("$", "USD ")
            .replace("¢", "cent ")
            // 版权和商标
            .replace("©", "(c) ")
            .replace("®", "(R) ")
            .replace("™", "(TM) ")
            // 其他常用符号
            .replace("°", " degrees ")
            .replace("℃", "C ")
            .replace("℉", "F ")
            .replace("§", "section ")
            .replace("¶", "paragraph ")
            .replace("†", "+ ")
            .replace("‡", "++ ")
            .replace("‰", " per mille ")
            .replace("‱", " per ten thousand ")
            .replace("′", "' ")
            .replace("″", "\" ")
            .replace("‴", "''' ")
            .replace("№", "No. ")
            .replace("℡", "Tel ")
            // 数学运算符
            .replace("√", "sqrt")
            .replace("∛", "cbrt")
            .replace("∜", "4thrt")
            .replace("∞", "infinity")
            .replace("∫", "integral")
            .replace("∂", "partial")
            .replace("∆", "delta")
            .replace("∇", "nabla")
            // 集合符号
            .replace("∈", " in ")
            .replace("∉", " not in ")
            .replace("∩", " intersect ")
            .replace("∪", " union ")
            .replace("⊂", " subset ")
            .replace("⊃", " superset ")
            .replace("⊆", " subset or equal ")
            .replace("⊇", " superset or equal ")
            .replace("∅", " empty set ")
            // 逻辑符号
            .replace("∀", " for all ")
            .replace("∃", " exists ")
            .replace("∄", " not exists ")
            .replace("∧", " and ")
            .replace("∨", " or ")
            .replace("¬", " not ")
            .replace("∴", " therefore ")
            .replace("∵", " because ")
            // 扑克牌符号
            .replace("♠", "spade ")
            .replace("♣", "club ")
            .replace("♥", "heart ")
            .replace("♦", "diamond ")
            // 音乐符号
            .replace("♩", "quarter note ")
            .replace("♪", "eighth note ")
            .replace("♫", "beamed eighth notes ")
            .replace("♬", "beamed sixteenth notes ")
            // 天气符号
            .replace("☀", "sun ")
            .replace("☁", "cloud ")
            .replace("☂", "umbrella ")
            .replace("☃", "snowman ")
            .replace("☄", "comet ")
            // 其他
            .replace("✓", "check ")
            .replace("✔", "check ")
            .replace("✗", "x ")
            .replace("✘", "x ")
            .replace("…", "...")
            .replace("–", "-")  // en dash
            .replace("—", "--")  // em dash
            .replace("'", "'")  // left single quote
            .replace("'", "'")  // right single quote
            .replace("\u201C", "\"")  // left double quote
            .replace("\u201D", "\"")  // right double quote
            .replace("„", "\"")  // double low quote
            .replace("‚", "'")  // single low quote
            .replace("«", "<<")  // left guillemet
            .replace("»", ">>")  // right guillemet
            .replace("‹", "<")  // left single guillemet
            .replace("›", ">")  // right single guillemet
            .replace("¡", "!")  // inverted exclamation
            .replace("¿", "?")  // inverted question
            .replace("‾", "~")  // overline
            .replace("‿", "_")  // undertie
            .replace("⁀", "^")  // character tie
            .replace("⁄", "/")  // fraction slash
            .replace("⁊", "&")  // tironian et
            .replace("⁌", "<<")  // left-pointing angle bracket
            .replace("⁍", ">>")  // right-pointing angle bracket
            .replace("⁎", "*")  // low asterisk
            .replace("⁏", ";")  // reversed semicolon
            .replace("⁐", ":")  // close up
            .replace("⁑", "**")  // two asterisks
            .replace("⁒", "***")  // three asterisks
            .replace("⁓", "~")  // swung dash
            .replace("⁔", ":")  // inverted undertie
            .replace("⁕", "*")  // flower punctuation mark
            .replace("⁖", "...")  // three dot punctuation
            .replace("⁗", "''''")  // quadruple prime
            .replace("⁘", "****")  // four dot punctuation
            .replace("⁙", "*****")  // five dot punctuation
            .replace("⁚", ":")  // two dot punctuation
            .replace("⁛", "***")  // three dot leader
            .replace("⁜", "****")  // four dot leader
            .replace("⁝", ":")  // tricolon
            .replace("⁞", ":")  // vertical four dots
            .trim();  // 移除首尾空格
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
