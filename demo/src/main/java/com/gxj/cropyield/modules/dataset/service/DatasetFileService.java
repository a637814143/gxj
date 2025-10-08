package com.gxj.cropyield.modules.dataset.service;

import com.gxj.cropyield.modules.dataset.dto.DatasetFileRequest;
import com.gxj.cropyield.modules.dataset.entity.DatasetFile;

import java.util.List;

public interface DatasetFileService {

    List<DatasetFile> listAll();

    DatasetFile create(DatasetFileRequest request);
}
