package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface YieldRecordRepository extends JpaRepository<YieldRecord, Long> {

    List<YieldRecord> findByRegionIdAndCropIdOrderByYearAsc(Long regionId, Long cropId);

    Optional<YieldRecord> findByCropIdAndRegionIdAndYear(Long cropId, Long regionId, Integer year);
}
