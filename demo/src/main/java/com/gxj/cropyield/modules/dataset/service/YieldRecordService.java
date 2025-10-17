package com.gxj.cropyield.modules.dataset.service;

import com.gxj.cropyield.modules.dataset.dto.YieldRecordDetailView;
import com.gxj.cropyield.modules.dataset.dto.YieldRecordRequest;

import java.util.List;

public interface YieldRecordService {

    List<YieldRecordDetailView> listAll();

    YieldRecordDetailView create(YieldRecordRequest request);
}
