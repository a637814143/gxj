package com.gxj.cropyield.modules.dataset.repository;

import com.gxj.cropyield.modules.dataset.entity.YieldRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YieldRecordRepository extends JpaRepository<YieldRecord, Long> {
}
