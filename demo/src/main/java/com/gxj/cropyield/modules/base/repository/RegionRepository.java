package com.gxj.cropyield.modules.base.repository;

import com.gxj.cropyield.modules.base.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("cropyieldRegionRepository")
public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByCode(String code);

    Optional<Region> findByNameIgnoreCase(String name);
}
