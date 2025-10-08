package com.gxj.cropyield.modules.dataset.service.impl;

import com.gxj.cropyield.modules.dataset.dto.DatasetFileRequest;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile;
import com.gxj.cropyield.modules.dataset.repository.DatasetFileRepository;
import com.gxj.cropyield.modules.dataset.service.DatasetFileService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatasetFileServiceImpl implements DatasetFileService {

    private final DatasetFileRepository datasetFileRepository;

    public DatasetFileServiceImpl(DatasetFileRepository datasetFileRepository) {
        this.datasetFileRepository = datasetFileRepository;
    }

    @Override
    public List<DatasetFile> listAll() {
        return datasetFileRepository.findAll();
    }

    @Override
    public DatasetFile create(DatasetFileRequest request) {
        DatasetFile datasetFile = new DatasetFile();
        datasetFile.setName(request.name());
        datasetFile.setType(request.type());
        datasetFile.setStoragePath(request.storagePath());
        datasetFile.setDescription(request.description());
        return datasetFileRepository.save(datasetFile);
    }
}
