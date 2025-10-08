package com.dali.cropyield.modules.base.repository;

import com.dali.cropyield.modules.base.entity.Region;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByCode(String code);

    List<Region> findByLevel(String level);
}
