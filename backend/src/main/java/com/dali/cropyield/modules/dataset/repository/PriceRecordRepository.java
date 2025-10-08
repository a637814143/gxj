package com.dali.cropyield.modules.dataset.repository;

import com.dali.cropyield.modules.dataset.entity.PriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {
}
