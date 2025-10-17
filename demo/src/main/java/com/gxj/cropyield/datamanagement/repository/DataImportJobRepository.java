package com.gxj.cropyield.datamanagement.repository;

import com.gxj.cropyield.datamanagement.model.DataImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DataImportJobRepository extends JpaRepository<DataImportJob, Long>, JpaSpecificationExecutor<DataImportJob> {

    Optional<DataImportJob> findByTaskId(String taskId);
}
