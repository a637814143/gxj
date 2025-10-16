package com.gxj.cropyield.modules.base.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.dto.RegionRequest;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.base.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;

    public RegionServiceImpl(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    public List<Region> listAll() {
        return regionRepository.findAll();
    }

    @Override
    @Transactional
    public Region create(RegionRequest request) {
        regionRepository.findByCode(request.code()).ifPresent(region -> {
            throw new BusinessException(ResultCode.BAD_REQUEST, "区域编码已存在");
        });

        Region region = new Region();
        region.setCode(request.code());
        region.setName(request.name());
        region.setLevel(request.level());
        region.setParentCode(request.parentCode());
        region.setParentName(request.parentName());
        region.setDescription(request.description());
        return regionRepository.save(region);
    }
}
