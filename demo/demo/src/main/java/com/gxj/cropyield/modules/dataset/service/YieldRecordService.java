package com.gxj.cropyield.modules.dataset.service;

import com.gxj.cropyield.modules.dataset.dto.YieldRecordRequest;
import com.gxj.cropyield.modules.dataset.entity.YieldRecord;

import java.util.List;
/**
 * 数据集管理模块的业务接口（接口），定义数据集管理相关的核心业务操作。
 * <p>核心方法：listAll、create。</p>
 */

public interface YieldRecordService {

    List<YieldRecord> listAll();

    YieldRecord create(YieldRecordRequest request);
}
