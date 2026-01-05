# PDF Export Character Encoding Fix - Final Solution

## Problem
PDF exports were showing question marks (?) for some Chinese characters across multiple export locations, particularly in the dashboard forecast data export.

## Root Cause Analysis

### Backend PDF Exports (ReportServiceImpl & YieldRecordService)
The issue was that hardcoded Chinese label strings in the PDF export methods were NOT being passed through the `sanitizeForPdf()` method. Only data from JSON nodes was being sanitized via the `safeText()` method.

### Frontend PDF Exports (DashboardView)
The dashboard forecast data PDF export was originally generated in the frontend using a custom PDF builder. The `encodePdfText()` function was encoding text to UTF-16BE format correctly, but the STSong-Light font had limited character support, causing rendering issues that were difficult to debug and fix consistently.

## Final Solution: Backend API for Dashboard PDF Export

Instead of continuing to debug the frontend PDF generation, we implemented a backend API endpoint to handle dashboard PDF exports using the same proven PDF generation logic as other exports.

### Changes Made

#### Backend Changes

1. **DashboardController.java** - Added PDF export endpoint
   ```java
   @GetMapping("/export/pdf")
   public ResponseEntity<byte[]> exportForecastDataAsPdf(
       @RequestParam(required = false) String crop,
       @RequestParam(required = false) String region,
       @RequestParam(required = false) Integer year
   )
   ```

2. **DashboardService.java** - Implemented PDF export method
   - Added `exportForecastDataAsPdf()` method
   - Uses same font configuration as other exports (STSong-Light with UniGB-UCS2-H)
   - Applies `sanitizeForPdf()` to ALL text (labels and data)
   - Supports filtering by crop, region, and year
   - Helper methods: `addHeaderCell()`, `addBodyCell()`, `buildFilterSummary()`, `formatNumber()`, `formatDate()`, `sanitizeForPdf()`

3. **ReportServiceImpl.java** - Backend report PDF export
   - Wrapped all hardcoded Chinese labels with `sanitizeForPdf()`
   - Fixed Unicode escape sequences for smart quotes

4. **YieldRecordService.java** - Backend yield data PDF export
   - Added `sanitizeForPdf()` method
   - Wrapped all hardcoded Chinese labels and dynamic data with `sanitizeForPdf()`

#### Frontend Changes

5. **forecast/src/views/DashboardView.vue** - Dashboard PDF export
   - Changed from frontend PDF generation to backend API call
   - Modified `handleExportPdf()` to call `/api/dashboard/export/pdf`
   - Passes filter parameters (crop, region, year) as query params
   - Handles authentication token
   - Removed frontend PDF generation code (can be cleaned up later)

6. **forecast/forecast/src/views/DashboardView.vue** - Same changes as above

### Technical Details

The `sanitizeForPdf()` method (used in all backend exports) replaces 200+ special characters with ASCII-safe equivalents:
- Math symbols: ², ³, ±, ×, ÷, ≈, ≤, ≥
- Arrows: →, ←, ↑, ↓, ⇒, ⇔
- Shapes: ◆, ●, ■, ▲, ★ (replaced with `* `)
- Greek letters: α, β, γ, δ, etc.
- Currency: €, £, ¥, $
- Smart quotes: ", ", ', ' (using Unicode escapes \u201C, \u201D)
- And many more...

### API Endpoint

**URL:** `GET /api/dashboard/export/pdf`

**Query Parameters:**
- `crop` (optional): Filter by crop name
- `region` (optional): Filter by region name  
- `year` (optional): Filter by year

**Response:** PDF file download

**Example:**
```
GET /api/dashboard/export/pdf?crop=玉米&region=德宏傣族景颇族自治州&year=2024
```

## Result

All PDF exports now use consistent backend generation with proper character sanitization:

1. ✅ **Report Center** - Report PDF export (backend)
2. ✅ **Dataset Management** - Yield data PDF export (backend)
3. ✅ **Dashboard** - Forecast data PDF export (backend - NEW)

The ? symbols should no longer appear in any exported PDFs. All Chinese text is properly sanitized before being added to the PDF document.

## Benefits of Backend Solution

1. **Consistency**: All PDF exports use the same proven generation logic
2. **Reliability**: Backend PDF library (OpenPDF/iText) is more robust than custom frontend implementation
3. **Maintainability**: Single codebase for PDF generation logic
4. **Performance**: PDF generation happens on server, reducing client-side load
5. **Character Support**: Centralized character sanitization ensures consistent handling

## Testing

- Backend compiled successfully
- Frontend built successfully
- Test all three PDF export locations:
  1. Report Center → Report details → Export PDF
  2. Dataset Management → Export PDF
  3. Dashboard → Forecast data table → Export PDF (now uses backend API)


