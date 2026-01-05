# 导出功能优化说明

## 问题描述

1. **PDF 导出中出现问号（�）**：中文字符显示为问号，这是字体编码问题
2. **Excel 导出格式混乱**：报告导出的 Excel 文件缺少格式化，数据排列混乱，难以阅读

## 解决方案

### 1. PDF 中文字体问题

**问题原因**：
- 使用的字体 `STSongStd-Light` 可能不存在或未正确嵌入
- OpenPDF 库需要正确的中文字体支持

**解决方法**：
- 将字体名称从 `STSongStd-Light` 改为 `STSong-Light`
- 使用 `UniGB-UCS2-H` 编码确保中文正确显示

**修改文件**：
1. `demo/src/main/java/com/gxj/cropyield/modules/report/service/impl/ReportServiceImpl.java`
2. `demo/src/main/java/com/gxj/cropyield/yielddata/YieldRecordService.java`

**修改内容**：
```java
// 修改前
BaseFont baseFont = BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);

// 修改后
BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
```

### 2. Excel 格式优化

**优化内容**：

#### 2.1 添加样式
- **标题样式**：16号粗体，居中对齐
- **章节标题样式**：14号粗体，浅蓝色背景
- **表头样式**：12号粗体，灰色背景，边框，居中对齐
- **数字格式**：保留两位小数

#### 2.2 改进布局
- 添加标题行并合并单元格
- 章节标题独占一行并合并单元格
- 章节间添加空行分隔
- 重点洞察部分使用合并单元格显示

#### 2.3 优化列宽
- 自动调整列宽
- 设置最小列宽（3000）和最大列宽（15000）
- 避免列宽过窄或过宽

#### 2.4 数据格式化
- 数字类型使用数字格式（保留两位小数）
- 日期类型使用日期格式
- 文本类型正常显示

**修改文件**：
`demo/src/main/java/com/gxj/cropyield/modules/report/service/impl/ReportServiceImpl.java`

**新增方法**：
```java
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
```

## 优化效果

### PDF 导出
- ✅ 中文字符正常显示，不再出现问号
- ✅ 表格布局清晰，数据对齐
- ✅ 字体大小合适，易于阅读

### Excel 导出

**优化前**：
- 数据混乱，没有明确的章节分隔
- 缺少样式，难以区分标题和数据
- 列宽不合适，内容显示不完整
- 数字格式不统一

**优化后**：
- ✅ 标题行醒目，居中显示
- ✅ 章节标题使用彩色背景，易于识别
- ✅ 表头使用灰色背景和边框
- ✅ 数字格式统一（保留两位小数）
- ✅ 章节间有空行分隔
- ✅ 列宽自动调整，内容完整显示
- ✅ 重点洞察使用合并单元格，排版美观

## 技术细节

### Excel 样式定义

```java
// 标题样式
CellStyle titleStyle = workbook.createCellStyle();
Font titleFont = workbook.createFont();
titleFont.setBold(true);
titleFont.setFontHeightInPoints((short) 16);
titleStyle.setFont(titleFont);
titleStyle.setAlignment(HorizontalAlignment.CENTER);

// 表头样式
CellStyle headerStyle = workbook.createCellStyle();
Font headerFont = workbook.createFont();
headerFont.setBold(true);
headerFont.setFontHeightInPoints((short) 12);
headerStyle.setFont(headerFont);
headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
headerStyle.setAlignment(HorizontalAlignment.CENTER);
headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
headerStyle.setBorderBottom(BorderStyle.THIN);
headerStyle.setBorderTop(BorderStyle.THIN);
headerStyle.setBorderLeft(BorderStyle.THIN);
headerStyle.setBorderRight(BorderStyle.THIN);

// 章节标题样式
CellStyle sectionHeaderStyle = workbook.createCellStyle();
Font sectionFont = workbook.createFont();
sectionFont.setBold(true);
sectionFont.setFontHeightInPoints((short) 14);
sectionHeaderStyle.setFont(sectionFont);
sectionHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
sectionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

// 数字格式
CellStyle numberStyle = workbook.createCellStyle();
numberStyle.setDataFormat(creationHelper.createDataFormat().getFormat("0.00"));
```

### 合并单元格示例

```java
// 标题行合并
sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

// 章节标题合并
sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 5));

// 重点洞察合并
sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 2));
```

### 列宽调整

```java
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
```

## 测试建议

### PDF 导出测试
1. 导出包含中文的报告，检查是否有问号
2. 检查表格对齐和字体大小
3. 验证数字格式是否正确

### Excel 导出测试
1. 打开导出的 Excel 文件
2. 检查标题行是否居中且醒目
3. 检查章节标题是否有背景色
4. 检查表头是否有灰色背景和边框
5. 检查数字是否保留两位小数
6. 检查列宽是否合适
7. 检查章节间是否有空行分隔

## 注意事项

1. **字体依赖**：PDF 导出依赖系统中的中文字体，如果 `STSong-Light` 不可用，可能需要安装或使用其他字体
2. **性能考虑**：大量数据导出时，合并单元格和样式设置可能影响性能
3. **兼容性**：导出的 Excel 文件使用 XLSX 格式，兼容 Excel 2007 及以上版本

## 后续优化建议

1. 添加图表导出功能
2. 支持自定义导出模板
3. 添加导出进度提示
4. 支持批量导出
5. 添加导出历史记录
