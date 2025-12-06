package com.gxj.cropyield.modules.consultation.entity;

import com.gxj.cropyield.common.model.BaseEntity;
import com.gxj.cropyield.modules.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 在线咨询模块的实体类，记录会话中的消息内容。
 */
@Entity
@Table(name = "consultation_message")
public class ConsultationMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "sender_role", length = 64)
    private String senderRole;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean recalled = false;

    @Column(name = "recalled_at")
    private LocalDateTime recalledAt;

    public Consultation getConsultation() {
        return consultation;
    }

    public void setConsultation(Consultation consultation) {
        this.consultation = consultation;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isRecalled() {
        return recalled;
    }

    public void setRecalled(boolean recalled) {
        this.recalled = recalled;
    }

    public LocalDateTime getRecalledAt() {
        return recalledAt;
    }

    public void setRecalledAt(LocalDateTime recalledAt) {
        this.recalledAt = recalledAt;
    }

}
