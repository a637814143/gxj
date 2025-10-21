package com.gxj.cropyield.modules.auth.service;

import com.gxj.cropyield.modules.auth.dto.LoginLogQuery;
import com.gxj.cropyield.modules.auth.dto.LoginLogRequest;
import com.gxj.cropyield.modules.auth.dto.LoginLogResponse;
import com.gxj.cropyield.modules.auth.dto.LoginLogSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoginLogService {

    void record(String username, boolean success, String ipAddress, String userAgent, String message);

    Page<LoginLogResponse> search(LoginLogQuery query, Pageable pageable);

    LoginLogResponse get(Long id);

    LoginLogResponse create(LoginLogRequest request);

    LoginLogResponse update(Long id, LoginLogRequest request);

    void delete(Long id);

    LoginLogSummaryResponse summarize();
}
