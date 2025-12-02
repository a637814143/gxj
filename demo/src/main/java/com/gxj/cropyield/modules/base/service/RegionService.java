package com.gxj.cropyield.modules.base.service;

import com.gxj.cropyield.modules.base.dto.RegionRequest;
import com.gxj.cropyield.modules.base.entity.Region;

import java.util.List;
/**
 * 基础数据模块的业务接口（接口），定义基础数据相关的核心业务操作。
 * <p>核心方法：listAll、create、update、delete。</p>
 */

public interface RegionService {

    List<Region> listAll();

    Region create(RegionRequest request);

    Region update(Long id, RegionRequest request);

    void delete(Long id);

    Region updateVisibility(Long id, boolean hidden);
}
