package com.gxj.cropyield.modules.consultation.controller;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ApiResponse;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.consultation.dto.ConsultationAttachmentDownload;
import com.gxj.cropyield.modules.consultation.dto.ConsultationCreateRequest;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessageRequest;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessageResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationMessagesResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationPageResponse;
import com.gxj.cropyield.modules.consultation.dto.ConsultationSummary;
import com.gxj.cropyield.modules.consultation.dto.ConsultationUpdateRequest;
import com.gxj.cropyield.modules.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 在线咨询模块的控制器，用于暴露咨询会话相关的 REST 接口。
 * <p>核心方法：listConsultations、createConsultation、getMessages、sendMessage、sendMultipartMessage、updateConsultation、markAsRead、downloadAttachment。</p>
 */
@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping
    public ApiResponse<ConsultationPageResponse> listConsultations(@RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return ApiResponse.success(consultationService.listConsultations(page, pageSize));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ConsultationSummary> createConsultation(@Valid @RequestBody ConsultationCreateRequest request) {
        return ApiResponse.success(consultationService.createConsultation(request));
    }

    @GetMapping("/{consultationId}/messages")
    public ApiResponse<ConsultationMessagesResponse> getMessages(@PathVariable Long consultationId,
                                                                 @RequestParam(name = "pageSize", defaultValue = "100") int pageSize) {
        return ApiResponse.success(consultationService.getMessages(consultationId, pageSize));
    }

    @PostMapping(value = "/{consultationId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ConsultationMessageResponse> sendMessage(@PathVariable Long consultationId,
                                                                @Valid @RequestBody ConsultationMessageRequest request) {
        return ApiResponse.success(consultationService.sendMessage(consultationId, request));
    }

    @PostMapping(value = "/{consultationId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ConsultationMessageResponse> sendMultipartMessage(@PathVariable Long consultationId,
                                                                         @RequestPart(value = "content", required = false) String content,
                                                                         @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        return ApiResponse.success(consultationService.sendMessage(consultationId, content, attachments));
    }

    @PatchMapping("/{consultationId}")
    public ApiResponse<ConsultationSummary> updateConsultation(@PathVariable Long consultationId,
                                                               @RequestBody ConsultationUpdateRequest request) {
        return ApiResponse.success(consultationService.updateConsultation(consultationId, request));
    }

    @PostMapping("/{consultationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long consultationId) {
        consultationService.markAsRead(consultationId);
        return ApiResponse.success();
    }

    @GetMapping("/{consultationId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long consultationId,
                                                     @PathVariable Long attachmentId) {
        ConsultationAttachmentDownload download = consultationService.loadAttachment(consultationId, attachmentId);
        byte[] bytes;
        try (var inputStream = download.resource().getInputStream()) {
            bytes = StreamUtils.copyToByteArray(inputStream);
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "下载附件失败");
        }
        String filename = download.filename() != null && !download.filename().isBlank()
            ? download.filename()
            : "consultation-attachment";
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
            .contentType(download.contentType() != null ? MediaType.parseMediaType(download.contentType()) : MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes);
    }
}
