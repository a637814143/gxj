package com.example.demo.yielddata;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/yields")
public class YieldRecordController {

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
}
