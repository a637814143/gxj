package com.gxj.cropyield.datamanagement.repository;

import com.gxj.cropyield.datamanagement.model.DataImportJobError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataImportJobErrorRepository extends JpaRepository<DataImportJobError, Long> {

    List<DataImportJobError> findTop10ByJobTaskIdOrderByCreatedAtAsc(String taskId);
}
