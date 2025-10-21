package com.gxj.cropyield.modules.auth.controller;

import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.common.response.PageResponse;
import com.gxj.cropyield.modules.auth.dto.LoginLogQuery;
import com.gxj.cropyield.modules.auth.dto.LoginLogRequest;
import com.gxj.cropyield.modules.auth.dto.LoginLogResponse;
import com.gxj.cropyield.modules.auth.dto.LoginLogSummaryResponse;
import com.gxj.cropyield.modules.auth.service.LoginLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/system/login-logs")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<LoginLogResponse>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) Boolean success,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        LoginLogQuery query = new LoginLogQuery(username, success, startDate, endDate);
        Page<LoginLogResponse> result = loginLogService.search(
            query,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ApiResponse.success(new PageResponse<>(result.getContent(), result.getTotalElements(), safePage, safeSize));
    }

    @GetMapping("/summary")
    public ApiResponse<LoginLogSummaryResponse> summary() {
        return ApiResponse.success(loginLogService.summarize());
    }

    @GetMapping("/{id}")
    public ApiResponse<LoginLogResponse> detail(@PathVariable("id") Long id) {
        return ApiResponse.success(loginLogService.get(id));
    }

    @PostMapping
    public ApiResponse<LoginLogResponse> create(@Valid @RequestBody LoginLogRequest request) {
        return ApiResponse.success(loginLogService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LoginLogResponse> update(@PathVariable("id") Long id, @Valid @RequestBody LoginLogRequest request) {
        return ApiResponse.success(loginLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        loginLogService.delete(id);
        return ApiResponse.success();
    }
}
