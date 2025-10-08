package com.dali.cropyield.modules.forecast.service.impl;

import com.dali.cropyield.common.exception.BusinessException;
import com.dali.cropyield.modules.base.entity.Crop;
import com.dali.cropyield.modules.base.entity.Region;
import com.dali.cropyield.modules.base.repository.CropRepository;
import com.dali.cropyield.modules.base.repository.RegionRepository;
import com.dali.cropyield.modules.forecast.dto.ForecastTaskRequest;
import com.dali.cropyield.modules.forecast.entity.ForecastModel;
import com.dali.cropyield.modules.forecast.entity.ForecastTask;
import com.dali.cropyield.modules.forecast.repository.ForecastModelRepository;
import com.dali.cropyield.modules.forecast.repository.ForecastTaskRepository;
import com.dali.cropyield.modules.forecast.service.ForecastTaskService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForecastTaskServiceImpl implements ForecastTaskService {

    private final ForecastTaskRepository forecastTaskRepository;
    private final CropRepository cropRepository;
    private final RegionRepository regionRepository;
    private final ForecastModelRepository forecastModelRepository;

    @Override
    public List<ForecastTask> findAll() {
        return forecastTaskRepository.findAll();
    }

    @Override
    @Transactional
    public ForecastTask create(ForecastTaskRequest request) {
        Crop crop = cropRepository.findById(request.cropId())
                .orElseThrow(() -> BusinessException.notFound("作物不存在"));
        Region region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> BusinessException.notFound("地区不存在"));

        ForecastTask task = new ForecastTask();
        task.setCrop(crop);
        task.setRegion(region);
        task.setStartYear(request.startYear());
        task.setEndYear(request.endYear());
        task.setHorizonYears(request.horizonYears());
        task.setStatus(ForecastTask.Status.CREATED);
        task.setStartedAt(LocalDateTime.now());
        if (request.modelId() != null) {
            ForecastModel model = forecastModelRepository.findById(request.modelId())
                    .orElseThrow(() -> BusinessException.notFound("预测模型不存在"));
            task.setModel(model);
        }
        return forecastTaskRepository.save(task);
    }
}
