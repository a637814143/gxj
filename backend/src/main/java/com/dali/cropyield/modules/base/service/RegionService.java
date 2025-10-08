package com.dali.cropyield.modules.base.service;

import com.dali.cropyield.modules.base.dto.RegionRequest;
import com.dali.cropyield.modules.base.entity.Region;
import java.util.List;
import java.util.Optional;

public interface RegionService {

    List<Region> findAll();

    List<Region> findByLevel(String level);

    Optional<Region> findById(Long id);

    Region create(RegionRequest request);
}
