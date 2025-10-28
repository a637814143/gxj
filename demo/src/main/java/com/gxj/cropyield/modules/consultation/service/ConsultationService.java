package com.gxj.cropyield.modules.consultation.service;

import com.gxj.cropyield.modules.consultation.dto.ConsultationCreateRequest;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessageRequest;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessageResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessagesResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationPageResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationSummary;
import com.gxj.cropyield.modules.consultation.dto.ConsultationUpdateRequest;
import com.gxj.cropyield.modules.consultation.dto.ConsultationDepartmentOption;

import java.util.List;

/**
 * 在线咨询模块的业务接口（接口），定义咨询会话相关的核心业务操作。
 */
public interface ConsultationService {

    ConsultationPageResponse listConsultations(int page, int pageSize, String status, String keyword);

    ConsultationSummary createConsultation(ConsultationCreateRequest request);

    ConsultationMessagesResponse getMessages(Long consultationId, int pageSize);

    ConsultationMessageResponse sendMessage(Long consultationId, ConsultationMessageRequest request);

    ConsultationSummary updateConsultation(Long consultationId, ConsultationUpdateRequest request);

    void markAsRead(Long consultationId);

    ConsultationSummary closeConsultation(Long consultationId);

    List<ConsultationDepartmentOption> listDepartments();
}
