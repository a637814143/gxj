package com.dali.cropyield.modules.dataset.service;

import com.dali.cropyield.modules.dataset.dto.YieldRecordRequest;
import com.dali.cropyield.modules.dataset.entity.YieldRecord;
import java.util.List;

public interface YieldRecordService {

    List<YieldRecord> findAll();

    YieldRecord create(YieldRecordRequest request);
}
