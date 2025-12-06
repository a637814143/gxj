package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
/**
 * 数据集管理模块的数据访问接口（接口），封装了对数据集管理相关数据表的持久化操作。
 */

public interface DatasetFileRepository extends JpaRepository<DatasetFile, Long> {

    Optional<DatasetFile> findByName(String name);
}
