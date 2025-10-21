package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.PriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

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
