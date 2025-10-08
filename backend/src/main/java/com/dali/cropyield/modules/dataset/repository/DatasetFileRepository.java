package com.dali.cropyield.modules.dataset.repository;

import com.dali.cropyield.modules.dataset.entity.DatasetFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetFileRepository extends JpaRepository<DatasetFile, Long> {
}
