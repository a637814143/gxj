package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.WeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 气象数据仓储。
 */
public interface WeatherRecordRepository extends JpaRepository<WeatherRecord, Long>, JpaSpecificationExecutor<WeatherRecord> {
}
