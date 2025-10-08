package com.gxj.cropyield.modules.forecast.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.base.entity.Crop;
import com.gxj.cropyield.modules.base.entity.Region;
import com.gxj.cropyield.modules.base.repository.CropRepository;
import com.gxj.cropyield.modules.base.repository.RegionRepository;
import com.gxj.cropyield.modules.forecast.dto.ForecastTaskRequest;
import com.gxj.cropyield.modules.forecast.entity.ForecastModel;
import com.gxj.cropyield.modules.forecast.entity.ForecastTask;
import com.gxj.cropyield.modules.forecast.repository.ForecastModelRepository;
import com.gxj.cropyield.modules.forecast.repository.ForecastTaskRepository;
import com.gxj.cropyield.modules.forecast.service.ForecastTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ForecastTaskServiceImpl implements ForecastTaskService {

    private final ForecastTaskRepository forecastTaskRepository;
    private final ForecastModelRepository forecastModelRepository;
    private final CropRepository cropRepository;
    private final RegionRepository regionRepository;

    public ForecastTaskServiceImpl(ForecastTaskRepository forecastTaskRepository,
                                   ForecastModelRepository forecastModelRepository,
                                   CropRepository cropRepository,
                                   RegionRepository regionRepository) {
        this.forecastTaskRepository = forecastTaskRepository;
        this.forecastModelRepository = forecastModelRepository;
        this.cropRepository = cropRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    public List<ForecastTask> listAll() {
        return forecastTaskRepository.findAll();
    }

    @Override
    @Transactional
    public ForecastTask create(ForecastTaskRequest request) {
        ForecastModel model = forecastModelRepository.findById(request.modelId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "模型不存在"));
        Crop crop = cropRepository.findById(request.cropId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "作物不存在"));
        Region region = regionRepository.findById(request.regionId())
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "区域不存在"));

        ForecastTask task = new ForecastTask();
        task.setModel(model);
        task.setCrop(crop);
        task.setRegion(region);
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        task.setParameters(request.parameters());
        return forecastTaskRepository.save(task);
    }
}
