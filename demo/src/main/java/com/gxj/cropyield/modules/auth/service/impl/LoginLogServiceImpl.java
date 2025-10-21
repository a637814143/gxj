package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.dto.LoginLogQuery;
import com.gxj.cropyield.modules.auth.dto.LoginLogRequest;
import com.gxj.cropyield.modules.auth.dto.LoginLogResponse;
import com.gxj.cropyield.modules.auth.dto.LoginLogSummaryResponse;
import com.gxj.cropyield.modules.auth.entity.LoginLog;
import com.gxj.cropyield.modules.auth.repository.LoginLogRepository;
import com.gxj.cropyield.modules.auth.service.LoginLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public LoginLogServiceImpl(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    @Override
    public void record(String username, boolean success, String ipAddress, String userAgent, String message) {
        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setSuccess(success);
        log.setIpAddress(normalizeOptional(ipAddress, "UNKNOWN"));
        log.setUserAgent(normalizeOptional(userAgent, "UNKNOWN"));
        log.setMessage(message);
        loginLogRepository.save(log);
    }

    @Override
    public Page<LoginLogResponse> search(LoginLogQuery query, Pageable pageable) {
        Specification<LoginLog> specification = buildSpecification(query);

        Pageable effectivePageable = pageable;
        if (effectivePageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Page<LoginLog> page = loginLogRepository.findAll(specification, effectivePageable);
        List<LoginLogResponse> responses = page.getContent().stream()
            .map(LoginLogResponse::from)
            .toList();
        return new PageImpl<>(responses, effectivePageable, page.getTotalElements());
    }

    @Override
    public LoginLogResponse get(Long id) {
        return LoginLogResponse.from(resolveLog(id));
    }

    @Override
    public LoginLogResponse create(LoginLogRequest request) {
        LoginLog log = new LoginLog();
        applyRequest(log, request);
        return LoginLogResponse.from(loginLogRepository.save(log));
    }

    @Override
    public LoginLogResponse update(Long id, LoginLogRequest request) {
        LoginLog log = resolveLog(id);
        applyRequest(log, request);
        return LoginLogResponse.from(loginLogRepository.save(log));
    }

    @Override
    public void delete(Long id) {
        if (!loginLogRepository.existsById(id)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "登录日志不存在");
        }
        loginLogRepository.deleteById(id);
    }

    @Override
    public LoginLogSummaryResponse summarize() {
        long total = loginLogRepository.count();
        long successCount = loginLogRepository.countBySuccess(true);
        long failureCount = loginLogRepository.countBySuccess(false);

        Optional<LoginLog> latestSuccess = loginLogRepository.findFirstBySuccessOrderByCreatedAtDesc(true);
        Optional<LoginLog> latestFailure = loginLogRepository.findFirstBySuccessOrderByCreatedAtDesc(false);

        return new LoginLogSummaryResponse(
            total,
            successCount,
            failureCount,
            latestSuccess.map(LoginLog::getCreatedAt).orElse(null),
            latestSuccess.map(LoginLog::getUsername).orElse(null),
            latestFailure.map(LoginLog::getCreatedAt).orElse(null),
            latestFailure.map(LoginLog::getUsername).orElse(null)
        );
    }

    private Specification<LoginLog> buildSpecification(LoginLogQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null) {
                if (StringUtils.hasText(query.username())) {
                    predicates.add(cb.like(cb.lower(root.get("username")), "%" + query.username().trim().toLowerCase() + "%"));
                }
                if (query.success() != null) {
                    predicates.add(cb.equal(root.get("success"), query.success()));
                }
                LocalDate startDate = query.startDate();
                if (startDate != null) {
                    LocalDateTime startDateTime = startDate.atStartOfDay();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
                }
                LocalDate endDate = query.endDate();
                if (endDate != null) {
                    LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyRequest(LoginLog log, LoginLogRequest request) {
        log.setUsername(request.username());
        log.setSuccess(Boolean.TRUE.equals(request.success()));
        log.setIpAddress(normalizeOptional(request.ipAddress(), null));
        log.setUserAgent(normalizeOptional(request.userAgent(), null));
        log.setMessage(request.message());
    }

    private LoginLog resolveLog(Long id) {
        return loginLogRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "登录日志不存在"));
    }

    private String normalizeOptional(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }
}
