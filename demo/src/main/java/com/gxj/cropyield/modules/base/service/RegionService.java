package com.gxj.cropyield.modules.base.service;

import com.gxj.cropyield.modules.base.dto.RegionRequest;
import com.gxj.cropyield.modules.base.entity.Region;

import java.util.List;

public interface RegionService {

    List<Region> listAll();

    Region create(RegionRequest request);

    Region update(Long id, RegionRequest request);

    void delete(Long id);
}
