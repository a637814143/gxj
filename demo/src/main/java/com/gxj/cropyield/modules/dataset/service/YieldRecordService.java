package com.gxj.cropyield.modules.dataset.service;

import com.gxj.cropyield.modules.dataset.dto.YieldRecordRequest;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;

import java.util.List;

public interface YieldRecordService {

    List<YieldRecord> listAll();

    YieldRecord create(YieldRecordRequest request);
}
