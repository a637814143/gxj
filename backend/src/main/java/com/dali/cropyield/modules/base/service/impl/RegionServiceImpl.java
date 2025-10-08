package com.dali.cropyield.modules.base.service.impl;

import com.dali.cropyield.common.exception.BusinessException;
import com.dali.cropyield.modules.base.dto.RegionRequest;
import com.dali.cropyield.modules.base.entity.Region;
import com.dali.cropyield.modules.base.repository.RegionRepository;
import com.dali.cropyield.modules.base.service.RegionService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;

    @Override
    public List<Region> findAll() {
        return regionRepository.findAll();
    }

    @Override
    public List<Region> findByLevel(String level) {
        return regionRepository.findByLevel(level);
    }

    @Override
    public Optional<Region> findById(Long id) {
        return regionRepository.findById(id);
    }

    @Override
    @Transactional
    public Region create(RegionRequest request) {
        Region region = new Region();
        region.setCode(request.code());
        region.setName(request.name());
        region.setLevel(request.level());
        if (request.parentId() != null) {
            Region parent = regionRepository.findById(request.parentId())
                    .orElseThrow(() -> BusinessException.notFound("父级地区不存在"));
            region.setParent(parent);
        }
        return regionRepository.save(region);
    }
}
