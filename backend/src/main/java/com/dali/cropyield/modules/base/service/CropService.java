package com.dali.cropyield.modules.base.service;

import com.dali.cropyield.modules.base.dto.CropRequest;
import com.dali.cropyield.modules.base.entity.Crop;
import java.util.List;
import java.util.Optional;

public interface CropService {

    List<Crop> findAll();

    Optional<Crop> findById(Long id);

    Crop create(CropRequest request);

    Crop update(Long id, CropRequest request);
}
