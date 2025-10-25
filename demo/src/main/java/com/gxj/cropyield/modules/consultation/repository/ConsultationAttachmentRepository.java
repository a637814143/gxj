package com.gxj.cropyield.modules.consultation.repository;

import com.gxj.cropyield.modules.consultation.entity.ConsultationAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 在线咨询模块的数据访问接口（接口），封装消息附件的持久化操作。
 */
public interface ConsultationAttachmentRepository extends JpaRepository<ConsultationAttachment, Long> {

    Optional<ConsultationAttachment> findByIdAndMessageConsultationId(Long id, Long consultationId);
}
