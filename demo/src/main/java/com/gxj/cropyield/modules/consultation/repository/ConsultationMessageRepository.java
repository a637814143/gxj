package com.gxj.cropyield.modules.consultation.repository;

import com.gxj.cropyield.modules.consultation.entity.ConsultationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 在线咨询模块的数据访问接口（接口），封装会话消息的持久化操作。
 */
public interface ConsultationMessageRepository extends JpaRepository<ConsultationMessage, Long> {

    List<ConsultationMessage> findByConsultationIdOrderByCreatedAtAsc(Long consultationId);

    Optional<ConsultationMessage> findFirstByConsultationIdOrderByCreatedAtDesc(Long consultationId);

    long countByConsultationIdAndCreatedAtAfterAndSenderIdNot(Long consultationId, LocalDateTime createdAt, Long senderId);

    long countByConsultationIdAndSenderIdNot(Long consultationId, Long senderId);

    long countByConsultationId(Long consultationId);
}
