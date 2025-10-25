package com.gxj.cropyield.modules.consultation.repository;

import com.gxj.cropyield.modules.auth.entity.User;
import com.gxj.cropyield.modules.consultation.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 在线咨询模块的数据访问接口（接口），封装咨询会话的持久化操作。
 */
public interface ConsultationRepository extends JpaRepository<Consultation, Long>, JpaSpecificationExecutor<Consultation> {

    Page<Consultation> findByCreatedBy(User user, Pageable pageable);
}
