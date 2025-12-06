package com.gxj.cropyield.modules.base.service;

import com.gxj.cropyield.modules.base.dto.CropRequest;
import com.gxj.cropyield.modules.base.entity.Crop;

import java.util.List;
/**
 * 基础数据模块的业务接口（接口），定义基础数据相关的核心业务操作。
 * <p>核心方法：listAll、create。</p>
 */

public interface CropService {

    List<Crop> listAll();

    Crop create(CropRequest request);
}
