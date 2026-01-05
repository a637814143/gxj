package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
/**
 * 数据集管理模块的数据访问接口（接口），封装了对数据集管理相关数据表的持久化操作。
 */

@Repository("cropyieldYieldRecordRepository")
public interface YieldRecordRepository extends JpaRepository<YieldRecord, Long> {

    @EntityGraph(attributePaths = {"crop", "region", "datasetFile"})
    List<YieldRecord> findByRegionIdAndCropIdOrderByYearAsc(Long regionId, Long cropId);

    Optional<YieldRecord> findByCropIdAndRegionIdAndYear(Long cropId, Long regionId, Integer year);

    List<YieldRecord> findTop5ByOrderByYearDesc();

    Long deleteByDatasetFileId(Long datasetFileId);

    boolean existsByCropId(Long cropId);

    boolean existsByRegionId(Long regionId);

    @Query("select distinct record.crop.id from YieldRecord record where record.datasetFile.id = :datasetFileId")
    List<Long> findDistinctCropIdsByDatasetFileId(@Param("datasetFileId") Long datasetFileId);

    @Query("select distinct record.region.id from YieldRecord record where record.datasetFile.id = :datasetFileId")
    List<Long> findDistinctRegionIdsByDatasetFileId(@Param("datasetFileId") Long datasetFileId);

    @EntityGraph(attributePaths = {"crop", "region", "datasetFile"})
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

    @EntityGraph(attributePaths = {"crop", "region"})
    List<YieldRecord> findByRegionId(Long regionId);
    
    // 用于仪表盘的优化查询 - 预加载所有关联实体
    @EntityGraph(attributePaths = {"crop", "region"})
    @Query("SELECT y FROM YieldRecord y ORDER BY y.year DESC")
    List<YieldRecord> findAllWithDetails();
}
