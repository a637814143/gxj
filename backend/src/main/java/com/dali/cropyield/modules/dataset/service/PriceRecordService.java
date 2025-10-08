package com.dali.cropyield.modules.dataset.service;

import com.dali.cropyield.modules.dataset.dto.PriceRecordRequest;
import com.dali.cropyield.modules.dataset.entity.PriceRecord;
import java.util.List;

public interface PriceRecordService {

    List<PriceRecord> findAll();

    PriceRecord create(PriceRecordRequest request);
}
