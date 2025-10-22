package com.gxj.cropyield.modules.base.repository;

import com.gxj.cropyield.modules.base.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
/**
 * 基础数据模块的数据访问接口（接口），封装了对基础数据相关数据表的持久化操作。
 */

@Repository("cropyieldCropRepository")
public interface CropRepository extends JpaRepository<Crop, Long> {
    Optional<Crop> findByCode(String code);

    Optional<Crop> findByNameIgnoreCase(String name);
}
