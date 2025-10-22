package com.gxj.cropyield.modules.base.repository;

import com.gxj.cropyield.modules.base.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
/**
 * 基础数据模块的数据访问接口（接口），封装了对基础数据相关数据表的持久化操作。
 */

@Repository("cropyieldRegionRepository")
public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByCode(String code);

    Optional<Region> findByNameIgnoreCase(String name);
}
