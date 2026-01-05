package com.gxj.cropyield.yielddata;

import com.gxj.cropyield.modules.dataset.dto.YieldRecordResponse;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import com.gxj.cropyield.modules.dataset.repository.YieldRecordRepository;
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
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
/**
 * 产量数据模块的业务接口，定义产量数据相关的核心业务操作。
 * <p>核心方法：search、exportAsExcel、exportAsPdf、setNumeric、addHeaderCell、addBodyCell、buildFilterSummary、formatNumber。</p>
 */

@Service
public class YieldRecordService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final YieldRecordRepository yieldRecordRepository;

    public YieldRecordService(YieldRecordRepository yieldRecordRepository) {
        this.yieldRecordRepository = yieldRecordRepository;
    }

    public List<YieldRecordResponse> search(Long cropId, Long regionId, Integer startYear, Integer endYear) {
        return yieldRecordRepository.search(cropId, regionId, startYear, endYear).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public byte[] exportAsExcel(Long cropId, Long regionId, Integer startYear, Integer endYear) {
        List<YieldRecord> records = yieldRecordRepository.search(cropId, regionId, startYear, endYear);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("云南农作物产量数据");
            CreationHelper creationHelper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));

            String[] headers = new String[]{"作物", "类别", "地区", "地区级别", "年份", "播种面积(千公顷)", "产量(万吨)", "单产(吨/公顷)", "采集日期", "数据来源"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (YieldRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getCrop().getName());
                row.createCell(1).setCellValue(nullToEmpty(record.getCrop().getCategory()));
                row.createCell(2).setCellValue(record.getRegion().getName());
                row.createCell(3).setCellValue(nullToEmpty(record.getRegion().getLevel()));
                row.createCell(4).setCellValue(record.getYear());
                setNumeric(row, 5, record.getSownArea());
                setNumeric(row, 6, record.getProduction());
                setNumeric(row, 7, record.getYieldPerHectare());

                Cell collectedAtCell = row.createCell(8);
                if (record.getCollectedAt() != null) {
                    collectedAtCell.setCellValue(java.sql.Date.valueOf(record.getCollectedAt()));
                    collectedAtCell.setCellStyle(dateStyle);
                }
                row.createCell(9).setCellValue(nullToEmpty(record.getDataSource()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("导出 Excel 失败", exception);
        }
    }

    public byte[] exportAsPdf(Long cropId, Long regionId, Integer startYear, Integer endYear) {
        List<YieldRecord> records = yieldRecordRepository.search(cropId, regionId, startYear, endYear);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font headerFont = new Font(baseFont, 11, Font.BOLD);
            Font bodyFont = new Font(baseFont, 10);

            Paragraph title = new Paragraph(sanitizeForPdf("云南农作物产量数据导出"), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(sanitizeForPdf("导出时间：" + DATE_FORMAT.format(LocalDate.now())), bodyFont));
            document.add(new Paragraph(sanitizeForPdf(buildFilterSummary(records, cropId, regionId, startYear, endYear)), bodyFont));
            document.add(new Paragraph(sanitizeForPdf("记录总数：" + records.size()), bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{18, 13, 18, 12, 8, 14, 14, 14, 14, 18});

            addHeaderCell(table, sanitizeForPdf("作物"), headerFont);
            addHeaderCell(table, sanitizeForPdf("类别"), headerFont);
            addHeaderCell(table, sanitizeForPdf("地区"), headerFont);
            addHeaderCell(table, sanitizeForPdf("地区级别"), headerFont);
            addHeaderCell(table, sanitizeForPdf("年份"), headerFont);
            addHeaderCell(table, sanitizeForPdf("播种面积(千公顷)"), headerFont);
            addHeaderCell(table, sanitizeForPdf("产量(万吨)"), headerFont);
            addHeaderCell(table, sanitizeForPdf("单产(吨/公顷)"), headerFont);
            addHeaderCell(table, sanitizeForPdf("采集日期"), headerFont);
            addHeaderCell(table, sanitizeForPdf("数据来源"), headerFont);

            for (YieldRecord record : records) {
                addBodyCell(table, sanitizeForPdf(record.getCrop().getName()), bodyFont);
                addBodyCell(table, sanitizeForPdf(nullToEmpty(record.getCrop().getCategory())), bodyFont);
                addBodyCell(table, sanitizeForPdf(record.getRegion().getName()), bodyFont);
                addBodyCell(table, sanitizeForPdf(nullToEmpty(record.getRegion().getLevel())), bodyFont);
                addBodyCell(table, String.valueOf(record.getYear()), bodyFont);
                addBodyCell(table, formatNumber(record.getSownArea()), bodyFont);
                addBodyCell(table, formatNumber(record.getProduction()), bodyFont);
                addBodyCell(table, formatNumber(record.getYieldPerHectare()), bodyFont);
                addBodyCell(table, formatDate(record.getCollectedAt()), bodyFont);
                addBodyCell(table, sanitizeForPdf(nullToEmpty(record.getDataSource())), bodyFont);
            }

            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("导出 PDF 失败", exception);
        }
    }

    private void setNumeric(Row row, int columnIndex, Double value) {
        if (value == null) {
            row.createCell(columnIndex).setBlank();
        } else {
            row.createCell(columnIndex).setCellValue(value);
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

    private String buildFilterSummary(List<YieldRecord> records, Long cropId, Long regionId, Integer startYear, Integer endYear) {
        String cropPart;
        if (cropId == null) {
            cropPart = "作物：全部";
        } else {
            cropPart = records.stream()
                    .map(record -> record.getCrop().getName())
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .map(name -> "作物：" + name)
                    .orElse("作物ID：" + cropId);
        }

        String regionPart;
        if (regionId == null) {
            regionPart = "地区：全部";
        } else {
            regionPart = records.stream()
                    .map(record -> record.getRegion().getName())
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst()
                    .map(name -> "地区：" + name)
                    .orElse("地区ID：" + regionId);
        }

        String yearPart = String.format(Locale.CHINA, "年份：%s-%s",
                startYear == null ? "全部" : startYear,
                endYear == null ? "全部" : endYear);
        return String.join("，", cropPart, regionPart, yearPart);
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
        return DATE_FORMAT.format(date);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
    
    private String sanitizeForPdf(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // 替换常见的特殊字符为 PDF 友好的版本
        return text
            // 数学符号
            .replace("²", "2")
            .replace("³", "3")
            .replace("±", "+/-")
            .replace("×", "x")
            .replace("÷", "/")
            .replace("≈", "~")
            .replace("≤", "<=")
            .replace("≥", ">=")
            .replace("≠", "!=")
            // 箭头符号
            .replace("→", "->")
            .replace("←", "<-")
            .replace("↑", "^")
            .replace("↓", "v")
            // 项目符号和形状
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
            // 货币符号
            .replace("€", "EUR ")
            .replace("£", "GBP ")
            .replace("¥", "CNY ")
            .replace("$", "USD ")
            // 版权和商标
            .replace("©", "(c) ")
            .replace("®", "(R) ")
            .replace("™", "(TM) ")
            // 其他常用符号
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

    private YieldRecordResponse mapToResponse(YieldRecord record) {
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
                record.getDataSource(),
                record.getCollectedAt()
        );
    }
}
