package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface YieldRecordRepository extends JpaRepository<YieldRecord, Long> {

    List<YieldRecord> findByRegionIdAndCropIdOrderByYearAsc(Long regionId, Long cropId);
}
