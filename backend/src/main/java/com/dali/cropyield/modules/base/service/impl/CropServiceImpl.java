package com.dali.cropyield.modules.base.service.impl;

import com.dali.cropyield.common.exception.BusinessException;
import com.dali.cropyield.modules.base.dto.CropRequest;
import com.dali.cropyield.modules.base.entity.Crop;
import com.dali.cropyield.modules.base.repository.CropRepository;
import com.dali.cropyield.modules.base.service.CropService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CropServiceImpl implements CropService {

    private final CropRepository cropRepository;

    @Override
    public List<Crop> findAll() {
        return cropRepository.findAll();
    }

    @Override
    public Optional<Crop> findById(Long id) {
        return cropRepository.findById(id);
    }

    @Override
    @Transactional
    public Crop create(CropRequest request) {
        Crop crop = new Crop();
        crop.setName(request.name());
        crop.setCategory(request.category());
        crop.setUnit(request.unit());
        crop.setDescription(request.description());
        return cropRepository.save(crop);
    }

    @Override
    @Transactional
    public Crop update(Long id, CropRequest request) {
        Crop crop = cropRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("作物不存在"));
        crop.setName(request.name());
        crop.setCategory(request.category());
        crop.setUnit(request.unit());
        crop.setDescription(request.description());
        return cropRepository.save(crop);
    }
}
