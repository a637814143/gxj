package com.gxj.cropyield.modules.forecast.repository;

import com.gxj.cropyield.modules.forecast.entity.ModelRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 模型仓库表的持久层接口。
 */
public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, Long> {
}
