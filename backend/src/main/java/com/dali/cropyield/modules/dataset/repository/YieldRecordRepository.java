package com.dali.cropyield.modules.dataset.repository;

import com.dali.cropyield.modules.dataset.entity.YieldRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YieldRecordRepository extends JpaRepository<YieldRecord, Long> {
}
