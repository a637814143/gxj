package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface YieldRecordRepository extends JpaRepository<YieldRecord, Long> {

    List<YieldRecord> findByRegionIdAndCropIdOrderByYearAsc(Long regionId, Long cropId);

    Optional<YieldRecord> findByCropIdAndRegionIdAndYear(Long cropId, Long regionId, Integer year);

    List<YieldRecord> findTop5ByOrderByYearDesc();

    @Query("""
            SELECT record FROM YieldRecord record
            WHERE (:cropId IS NULL OR record.crop.id = :cropId)
              AND (:regionId IS NULL OR record.region.id = :regionId)
              AND (:startYear IS NULL OR record.year >= :startYear)
              AND (:endYear IS NULL OR record.year <= :endYear)
            ORDER BY record.year DESC, record.crop.name ASC, record.region.name ASC
            """)
    List<YieldRecord> search(
            @Param("cropId") Long cropId,
            @Param("regionId") Long regionId,
            @Param("startYear") Integer startYear,
            @Param("endYear") Integer endYear
    );
}
