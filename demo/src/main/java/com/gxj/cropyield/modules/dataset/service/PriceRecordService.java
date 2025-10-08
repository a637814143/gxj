package com.gxj.cropyield.modules.dataset.service;

import com.gxj.cropyield.modules.dataset.dto.PriceRecordRequest;
import com.gxj.cropyield.modules.dataset.entity.PriceRecord;

import java.util.List;

public interface PriceRecordService {

    List<PriceRecord> listAll();

    PriceRecord create(PriceRecordRequest request);
}
