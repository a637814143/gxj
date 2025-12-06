package com.gxj.cropyield.modules.consultation.repository;

import com.gxj.cropyield.modules.consultation.entity.ConsultationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 在线咨询模块的数据访问接口（接口），封装会话参与者的持久化操作。
 */
public interface ConsultationParticipantRepository extends JpaRepository<ConsultationParticipant, Long> {

    Optional<ConsultationParticipant> findByConsultationIdAndUserId(Long consultationId, Long userId);
}
