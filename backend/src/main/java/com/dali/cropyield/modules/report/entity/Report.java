package com.dali.cropyield.modules.report.entity;

import com.dali.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "report_archive")
public class Report extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 512)
    private String summary;

    @Column(length = 256)
    private String fileUrl;

    private LocalDateTime generatedAt;

    @Column(length = 64)
    private String generatedBy;

    @Column(length = 32)
    private String status;
}
