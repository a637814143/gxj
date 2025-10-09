package com.gxj.cropyield.yielddata;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/yields")
public class YieldRecordController {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument." +
                    "spreadsheetml.sheet"
    );

    private final YieldRecordService yieldRecordService;

    public YieldRecordController(YieldRecordService yieldRecordService) {
        this.yieldRecordService = yieldRecordService;
    }

    @GetMapping
    public List<YieldRecordResponse> list(
            @RequestParam(required = false) Long cropId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear
    ) {
        return yieldRecordService.search(cropId, regionId, startYear, endYear);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Long cropId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear
    ) {
        byte[] data = yieldRecordService.exportAsExcel(cropId, regionId, startYear, endYear);
        String filename = "云南农作物数据_" + LocalDate.now() + ".xlsx";
        return buildFileResponse(data, filename, EXCEL_MEDIA_TYPE);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) Long cropId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear
    ) {
        byte[] data = yieldRecordService.exportAsPdf(cropId, regionId, startYear, endYear);
        String filename = "云南农作物数据_" + LocalDate.now() + ".pdf";
        return buildFileResponse(data, filename, MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> buildFileResponse(byte[] data, String filename, MediaType mediaType) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(data);
    }
}
