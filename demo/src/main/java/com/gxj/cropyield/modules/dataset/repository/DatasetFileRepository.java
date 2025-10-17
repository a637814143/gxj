package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatasetFileRepository extends JpaRepository<DatasetFile, Long> {

    Optional<DatasetFile> findByName(String name);
}
