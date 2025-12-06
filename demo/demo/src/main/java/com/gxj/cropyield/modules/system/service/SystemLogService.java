package com.gxj.cropyield.modules.system.service;

import com.gxj.cropyield.modules.system.entity.SystemLog;
import com.gxj.cropyield.modules.system.repository.SystemLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 系统设置模块的业务接口，定义系统设置相关的核心业务操作。
 * <p>核心方法：record、list。</p>
 */
@Service
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    public SystemLogService(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    public void record(String username, String action, String detail) {
        SystemLog log = new SystemLog();
        log.setUsername(username);
        log.setAction(action);
        log.setDetail(detail);
        systemLogRepository.save(log);
    }

    public Page<SystemLog> list(Pageable pageable) {
        PageRequest request = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return systemLogRepository.findAll(request);
    }
}
