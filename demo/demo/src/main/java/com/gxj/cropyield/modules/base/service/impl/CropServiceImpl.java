package com.gxj.cropyield.modules.base.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.dto.CropRequest;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.service.CropService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * 基础数据模块的业务实现类，负责落实基础数据领域的业务处理逻辑。
 * <p>核心方法：listAll、create。</p>
 */

@Service
public class CropServiceImpl implements CropService {

    private final CropRepository cropRepository;

    public CropServiceImpl(CropRepository cropRepository) {
        this.cropRepository = cropRepository;
    }

    @Override
    public List<Crop> listAll() {
        return cropRepository.findAll();
    }

    @Override
    @Transactional
    public Crop create(CropRequest request) {
        cropRepository.findByCode(request.code()).ifPresent(crop -> {
            throw new BusinessException(ResultCode.BAD_REQUEST, "作物编码已存在");
        });

        Crop crop = new Crop();
        crop.setCode(request.code());
        crop.setName(request.name());
        crop.setCategory(request.category());
        crop.setDescription(request.description());
        return cropRepository.save(crop);
    }
}
