package com.gxj.cropyield.datamanagement.repository;

import com.gxj.cropyield.datamanagement.model.DataImportJobError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * 数据导入模块的数据访问接口（接口），封装了对数据导入相关数据表的持久化操作。
 */

public interface DataImportJobErrorRepository extends JpaRepository<DataImportJobError, Long> {

    List<DataImportJobError> findTop10ByJobTaskIdOrderByCreatedAtAsc(String taskId);
}
