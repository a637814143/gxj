package com.dali.cropyield.modules.dataset.service.impl;

import com.dali.cropyield.modules.dataset.entity.DatasetFile;
import com.dali.cropyield.modules.dataset.repository.DatasetFileRepository;
import com.dali.cropyield.modules.dataset.service.DatasetFileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatasetFileServiceImpl implements DatasetFileService {

    private final DatasetFileRepository datasetFileRepository;

    @Override
    public List<DatasetFile> findAll() {
        return datasetFileRepository.findAll();
    }

    @Override
    public DatasetFile save(DatasetFile datasetFile) {
        return datasetFileRepository.save(datasetFile);
    }
}
