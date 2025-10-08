package com.dali.cropyield.modules.dataset.service;

import com.dali.cropyield.modules.dataset.entity.DatasetFile;
import java.util.List;

public interface DatasetFileService {

    List<DatasetFile> findAll();

    DatasetFile save(DatasetFile datasetFile);
}
