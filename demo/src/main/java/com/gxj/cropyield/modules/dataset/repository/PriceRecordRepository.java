package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.PriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
/**
 * 数据集管理模块的数据访问接口（接口），封装了对数据集管理相关数据表的持久化操作。
 */

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    Long deleteByDatasetFileId(Long datasetFileId);

    boolean existsByCropId(Long cropId);

    boolean existsByRegionId(Long regionId);

    @Query("select distinct record.crop.id from PriceRecord record where record.datasetFile.id = :datasetFileId")
    List<Long> findDistinctCropIdsByDatasetFileId(@Param("datasetFileId") Long datasetFileId);

    @Query("select distinct record.region.id from PriceRecord record where record.datasetFile.id = :datasetFileId")
    List<Long> findDistinctRegionIdsByDatasetFileId(@Param("datasetFileId") Long datasetFileId);

    List<PriceRecord> findByRegionIdAndCropIdAndRecordDateBetweenOrderByRecordDateAsc(Long regionId, Long cropId, LocalDate start, LocalDate end);
}
