package com.dali.cropyield.modules.dataset.entity;

import com.dali.cropyield.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ag_dataset_file")
public class DatasetFile extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String fileName;

    @Column(nullable = false, length = 64)
    private String fileType;

    @Column(length = 256)
    private String storagePath;

    @Column(length = 256)
    private String description;

    private LocalDate importedAt;
}
