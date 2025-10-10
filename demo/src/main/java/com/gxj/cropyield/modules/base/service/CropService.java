package com.gxj.cropyield.modules.base.service;

import com.gxj.cropyield.modules.base.dto.CropRequest;
import com.gxj.cropyield.modules.base.entity.Crop;

import java.util.List;

public interface CropService {

    List<Crop> listAll();

    Crop create(CropRequest request);
}
