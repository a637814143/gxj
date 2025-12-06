package com.gxj.cropyield.modules.consultation.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 在线咨询模块的实体类，记录消息附件的存储信息。
 */
@Entity
@Table(name = "consultation_attachment")
public class ConsultationAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ConsultationMessage message;

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(name = "original_name", length = 256)
    private String originalName;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    public ConsultationMessage getMessage() {
        return message;
    }

    public void setMessage(ConsultationMessage message) {
        this.message = message;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
}
